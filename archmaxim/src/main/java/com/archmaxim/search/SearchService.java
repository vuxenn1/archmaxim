package com.archmaxim.search;

import com.archmaxim.config.ArchmaximProperties;
import com.archmaxim.dto.FileDto;
import com.archmaxim.dto.FileSearchRequest;
import com.archmaxim.entity.FileRecord;
import com.archmaxim.exception.BadRequestException;
import com.archmaxim.repository.CourseLecturerRepository;
import com.archmaxim.repository.EnrollmentRepository;
import com.archmaxim.repository.FileRecordRepository;
import com.archmaxim.security.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Semantic file search: the query is embedded and compared with file embeddings
 * by cosine distance. If the query names a course, its files get a ranking bonus.
 */
@Service
public class SearchService {

    private static final int MIN_CANDIDATES = 40;
    private static final int CANDIDATE_FACTOR = 4;

    private final EmbeddingClient embeddingClient;
    private final CourseCatalog catalog;
    private final QueryAnalyzer analyzer;
    private final FileRecordRepository fileRepository;
    private final CourseLecturerRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate readOnly;
    private final ArchmaximProperties.Search settings;

    public SearchService(EmbeddingClient embeddingClient,
                         CourseCatalog catalog,
                         FileRecordRepository fileRepository,
                         CourseLecturerRepository assignmentRepository,
                         EnrollmentRepository enrollmentRepository,
                         JdbcTemplate jdbcTemplate,
                         PlatformTransactionManager transactionManager,
                         ArchmaximProperties properties) {
        this.embeddingClient = embeddingClient;
        this.catalog = catalog;
        this.fileRepository = fileRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.settings = properties.search();
        this.analyzer = new QueryAnalyzer(settings.fuzzyThreshold());
    }

    public List<FileDto> search(FileSearchRequest request, AuthenticatedUser user) {
        if (request.limit() < 1 || request.limit() > settings.maxLimit()) {
            throw new BadRequestException("limit must be between 1 and " + settings.maxLimit() + ".");
        }

        List<Long> courseIds = request.mine() ? ownCourseIds(user) : null;
        if (courseIds != null && courseIds.isEmpty()) {
            return List.of();
        }

        QueryAnalysis analysis = analyzer.analyze(request.query(), catalog.courses());
        float[] queryVector = embeddingClient.embed(analysis.expand(request.query()));

        List<Hit> hits = findHits(VectorFormat.toLiteral(queryVector), request, courseIds, analysis);
        if (hits.isEmpty()) {
            return List.of();
        }

        Map<Long, FileRecord> files = fileRepository.findWithDetailsByIdIn(hits.stream().map(Hit::id).toList())
                .stream()
                .collect(Collectors.toMap(FileRecord::getId, Function.identity()));

        List<FileDto> results = new ArrayList<>(hits.size());
        for (Hit hit : hits) {
            FileRecord file = files.get(hit.id());
            if (file != null) {
                results.add(FileDto.from(file, relevance(hit.distance())));
            }
        }
        return results;
    }

    private List<Long> ownCourseIds(AuthenticatedUser user) {
        return switch (user.role()) {
            case LECTURER -> assignmentRepository.findCourseIdsByLecturerId(user.id());
            case STUDENT -> enrollmentRepository.findCourseIdsByStudentId(user.id());
            case ADMIN -> null;
        };
    }

    /**
     * The inner query orders by raw distance only, so Postgres can serve it from the
     * HNSW index. The outer query then applies the distance cut-off and the course
     * bonus to that candidate set.
     */
    private List<Hit> findHits(String vector, FileSearchRequest request, List<Long> courseIds, QueryAnalysis analysis) {
        int candidates = Math.max(request.limit() * CANDIDATE_FACTOR, MIN_CANDIDATES);
        List<Object> args = new ArrayList<>();

        StringBuilder inner = new StringBuilder("SELECT f.id, f.course_id, f.embedding <=> CAST(? AS vector) AS distance ")
                .append("FROM file_record f ");
        args.add(vector);
        if (request.departmentId() != null) {
            inner.append("JOIN course c ON c.id = f.course_id ");
        }
        inner.append("WHERE f.is_active AND f.embedding IS NOT NULL ");
        if (request.year() != null) {
            inner.append("AND f.year = ? ");
            args.add(request.year());
        }
        if (request.season() != null) {
            inner.append("AND f.season = ? ");
            args.add(request.season().name());
        }
        if (request.type() != null) {
            inner.append("AND f.type = ? ");
            args.add(request.type().name());
        }
        if (request.departmentId() != null) {
            inner.append("AND c.department_id = ? ");
            args.add(request.departmentId());
        }
        if (courseIds != null) {
            inner.append("AND f.course_id IN (").append(String.join(",", Collections.nCopies(courseIds.size(), "?"))).append(") ");
            args.addAll(courseIds);
        }
        inner.append("ORDER BY f.embedding <=> CAST(? AS vector) LIMIT ?");
        args.add(vector);
        args.add(candidates);

        String sql;
        if (analysis.hasCourse()) {
            sql = "SELECT id, distance FROM (" + inner + ") candidates "
                    + "WHERE distance < ? OR (course_id = ? AND distance < ?) "
                    + "ORDER BY distance - CASE WHEN course_id = ? THEN ? ELSE 0 END LIMIT ?";
            args.add(settings.maxDistance());
            args.add(analysis.courseId());
            args.add(settings.maxDistance() + settings.courseBoost());
            args.add(analysis.courseId());
            args.add(settings.courseBoost());
        } else {
            sql = "SELECT id, distance FROM (" + inner + ") candidates WHERE distance < ? ORDER BY distance LIMIT ?";
            args.add(settings.maxDistance());
        }
        args.add(request.limit());

        List<Hit> hits = readOnly.execute(tx -> {
            // HNSW returns at most ef_search rows, and filters are applied after the
            // index scan, so widen the search and let pgvector keep scanning.
            jdbcTemplate.queryForObject("SELECT set_config('hnsw.ef_search', ?, true)", String.class,
                    String.valueOf(Math.min(candidates, 1000)));
            jdbcTemplate.queryForObject("SELECT set_config('hnsw.iterative_scan', 'relaxed_order', true)", String.class);
            return jdbcTemplate.query(sql,
                    (rs, rowNum) -> new Hit(rs.getLong("id"), rs.getDouble("distance")),
                    args.toArray());
        });
        return hits == null ? List.of() : hits;
    }

    private static double relevance(double distance) {
        return Math.round((1 - distance) * 1000) / 1000.0;
    }

    private record Hit(long id, double distance) {
    }
}
