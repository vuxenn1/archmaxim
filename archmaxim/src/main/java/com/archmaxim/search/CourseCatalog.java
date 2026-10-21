package com.archmaxim.search;

import com.archmaxim.entity.Course;
import com.archmaxim.entity.CourseAlias;
import com.archmaxim.repository.CourseRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory copy of all courses and aliases for query analysis, so a search does
 * not reload them from the database. Rebuilt lazily after {@link #invalidate()}.
 */
@Component
public class CourseCatalog {

    private final CourseRepository courseRepository;
    private final TransactionTemplate readOnly;
    private final AtomicLong generation = new AtomicLong();
    private volatile List<CatalogCourse> snapshot;

    public CourseCatalog(CourseRepository courseRepository, PlatformTransactionManager transactionManager) {
        this.courseRepository = courseRepository;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    public List<CatalogCourse> courses() {
        List<CatalogCourse> current = snapshot;
        if (current != null) {
            return current;
        }
        long loadedGeneration = generation.get();
        List<CatalogCourse> loaded = load();
        // Only keep the result if nothing changed while it was loading.
        if (generation.get() == loadedGeneration) {
            snapshot = loaded;
        }
        return loaded;
    }

    public void invalidate() {
        generation.incrementAndGet();
        snapshot = null;
    }

    private List<CatalogCourse> load() {
        List<CatalogCourse> courses = readOnly.execute(status -> courseRepository.findAllByOrderByCodeAsc().stream()
                .map(CourseCatalog::toCatalogCourse)
                .toList());
        return courses == null ? List.of() : courses;
    }

    private static CatalogCourse toCatalogCourse(Course course) {
        List<String> aliases = course.getAliases().stream().map(CourseAlias::getAlias).toList();
        return CatalogCourse.of(course.getId(), course.getCode(), course.getName(), aliases);
    }
}
