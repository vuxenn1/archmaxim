package com.archmaxim.search;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SearchDataListener {

    private final CourseCatalog catalog;
    private final IndexingService indexingService;

    public SearchDataListener(CourseCatalog catalog, IndexingService indexingService) {
        this.catalog = catalog;
        this.indexingService = indexingService;
    }

    // After commit, so the rebuilt catalog and the new embeddings see the change.
    @TransactionalEventListener(fallbackExecution = true)
    void onCourseContentChanged(CourseContentChanged event) {
        catalog.invalidate();
        if (!event.courseIds().isEmpty()) {
            indexingService.reindexCourses(event.courseIds());
        }
    }
}
