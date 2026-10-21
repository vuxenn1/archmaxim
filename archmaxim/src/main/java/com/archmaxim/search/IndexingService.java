package com.archmaxim.search;

import com.archmaxim.dto.IndexStatus;
import com.archmaxim.exception.EmbeddingUnavailableException;
import com.archmaxim.repository.FileRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Computes and stores file embeddings. Uploads are indexed right away when the
 * embedding service is up; anything it missed is picked up by a background job,
 * either started by an admin or by the scheduled retry.
 */
@Service
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);
    private static final int BATCH_SIZE = 16;
    private static final String MISSING_EMBEDDINGS_SQL =
            "SELECT id FROM file_record WHERE is_active AND embedding IS NULL ORDER BY id";
    private static final String UPDATE_EMBEDDING_SQL =
            "UPDATE file_record SET embedding = CAST(? AS vector) WHERE id = ?";

    private final FileRecordRepository fileRepository;
    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate readOnly;
    private final TaskExecutor taskExecutor;
    private final Clock clock;

    private final AtomicBoolean jobRunning = new AtomicBoolean();
    private final AtomicBoolean outageReported = new AtomicBoolean();
    private volatile IndexStatus status = IndexStatus.idle();

    public IndexingService(FileRecordRepository fileRepository,
                           EmbeddingClient embeddingClient,
                           JdbcTemplate jdbcTemplate,
                           PlatformTransactionManager transactionManager,
                           @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                           Clock clock) {
        this.fileRepository = fileRepository;
        this.embeddingClient = embeddingClient;
        this.jdbcTemplate = jdbcTemplate;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    /**
     * Indexes one file immediately.
     *
     * @return false if the embedding service could not be reached
     */
    public boolean indexNow(long fileId) {
        try {
            indexBatch(List.of(fileId));
            serviceRecovered();
            return true;
        } catch (EmbeddingUnavailableException e) {
            log.warn("Embedding service unavailable, file {} will be indexed by the next retry", fileId);
            return false;
        }
    }

    /**
     * Starts a background job over active files: only those without an embedding,
     * or all of them when {@code all} is true.
     *
     * @return false if a job is already running
     */
    public boolean startJob(boolean all) {
        if (!jobRunning.compareAndSet(false, true)) {
            return false;
        }
        Supplier<List<Long>> ids = all ? fileRepository::findActiveIds : this::findMissingIds;
        try {
            taskExecutor.execute(() -> runJob(ids));
        } catch (RuntimeException e) {
            jobRunning.set(false);
            throw e;
        }
        return true;
    }

    public IndexStatus status() {
        return status;
    }

    public void reindexCourses(Collection<Long> courseIds) {
        List<Long> ids = List.copyOf(courseIds);
        taskExecutor.execute(() -> {
            List<Long> fileIds = fileRepository.findActiveIdsByCourseIds(ids);
            try {
                indexAll(fileIds);
                log.info("Re-indexed {} files after a change to courses {}", fileIds.size(), ids);
            } catch (EmbeddingUnavailableException e) {
                // Old embeddings stay in place; an admin can re-run the full index later.
                log.warn("Could not re-index courses {}: embedding service unavailable", ids);
            } catch (RuntimeException e) {
                log.error("Re-indexing courses {} failed", ids, e);
            }
        });
    }

    @Scheduled(initialDelayString = "${archmaxim.indexing.retry-interval}",
            fixedDelayString = "${archmaxim.indexing.retry-interval}")
    void retryMissingEmbeddings() {
        if (jobRunning.get() || findMissingIds().isEmpty()) {
            return;
        }
        startJob(false);
    }

    private void runJob(Supplier<List<Long>> idSource) {
        Instant startedAt = clock.instant();
        int total = 0;
        int processed = 0;
        int failed = 0;
        try {
            List<Long> ids = idSource.get();
            total = ids.size();
            status = new IndexStatus(true, total, 0, 0, startedAt, null);

            for (List<Long> batch : partition(ids)) {
                try {
                    indexBatch(batch);
                    processed += batch.size();
                    serviceRecovered();
                } catch (EmbeddingUnavailableException e) {
                    failed = total - processed;
                    reportOutage();
                    break;
                } catch (RuntimeException e) {
                    failed += batch.size();
                    log.error("Indexing failed for files {}", batch, e);
                }
                status = new IndexStatus(true, total, processed, failed, startedAt, null);
            }
            if (total > 0) {
                log.info("Indexing finished: {} of {} files indexed, {} failed", processed, total, failed);
            }
        } catch (RuntimeException e) {
            log.error("Indexing job stopped unexpectedly", e);
        } finally {
            status = new IndexStatus(false, total, processed, failed, startedAt, clock.instant());
            jobRunning.set(false);
        }
    }

    private void indexAll(List<Long> fileIds) {
        for (List<Long> batch : partition(fileIds)) {
            indexBatch(batch);
        }
    }

    private void indexBatch(List<Long> fileIds) {
        // Texts are built inside a transaction because they walk lazy relations;
        // the slow embedding call happens outside it.
        List<PendingText> pending = readOnly.execute(tx -> fileRepository.findForIndexing(fileIds).stream()
                .map(file -> new PendingText(file.getId(), SearchableText.of(file)))
                .toList());
        if (pending == null || pending.isEmpty()) {
            return;
        }

        List<float[]> vectors = embeddingClient.embedAll(pending.stream().map(PendingText::text).toList());

        List<Object[]> updates = new ArrayList<>(pending.size());
        for (int i = 0; i < pending.size(); i++) {
            updates.add(new Object[]{VectorFormat.toLiteral(vectors.get(i)), pending.get(i).fileId()});
        }
        jdbcTemplate.batchUpdate(UPDATE_EMBEDDING_SQL, updates);
    }

    private List<Long> findMissingIds() {
        return jdbcTemplate.queryForList(MISSING_EMBEDDINGS_SQL, Long.class);
    }

    private void reportOutage() {
        if (outageReported.compareAndSet(false, true)) {
            log.warn("Embedding service unavailable, files without embeddings will be retried later");
        } else {
            log.debug("Embedding service still unavailable");
        }
    }

    private void serviceRecovered() {
        if (outageReported.compareAndSet(true, false)) {
            log.info("Embedding service is reachable again");
        }
    }

    private static List<List<Long>> partition(List<Long> ids) {
        List<List<Long>> batches = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
            batches.add(ids.subList(start, Math.min(ids.size(), start + BATCH_SIZE)));
        }
        return batches;
    }

    private record PendingText(Long fileId, String text) {
    }
}
