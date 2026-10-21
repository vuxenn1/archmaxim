package com.archmaxim.search;

/**
 * What the query analyzer recognised in a search query. When a course was
 * recognised, its name and code are appended to the query before embedding,
 * and its files get a ranking bonus.
 */
public record QueryAnalysis(Long courseId, String expansion, Match match) {

    public enum Match {
        NONE,
        ALIAS,
        CODE,
        NAME
    }

    public static final QueryAnalysis NONE = new QueryAnalysis(null, "", Match.NONE);

    static QueryAnalysis of(CatalogCourse course, Match match) {
        return new QueryAnalysis(course.id(), course.expansion(), match);
    }

    public boolean hasCourse() {
        return courseId != null;
    }

    public String expand(String query) {
        return expansion.isEmpty() ? query : query + " " + expansion;
    }
}
