package com.archmaxim.search;

import java.util.Collection;
import java.util.List;

/**
 * Published when data that feeds search changes: course names, codes,
 * descriptions, aliases or department names.
 */
public record CourseContentChanged(List<Long> courseIds) {

    public CourseContentChanged {
        courseIds = List.copyOf(courseIds);
    }

    public static CourseContentChanged of(Long courseId) {
        return new CourseContentChanged(List.of(courseId));
    }

    public static CourseContentChanged of(Collection<Long> courseIds) {
        return new CourseContentChanged(List.copyOf(courseIds));
    }
}
