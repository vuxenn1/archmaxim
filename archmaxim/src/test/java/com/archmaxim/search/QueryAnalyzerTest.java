package com.archmaxim.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryAnalyzerTest {

    private static final CatalogCourse PROGRAMMING_1 =
            CatalogCourse.of(1, "BIL101", "Bilgisayar Programlama I", List.of("BP1"));
    private static final CatalogCourse PROGRAMMING_2 =
            CatalogCourse.of(2, "BIL201", "Bilgisayar Programlama II", List.of("BP2", "BP2 Lab"));
    private static final CatalogCourse AI =
            CatalogCourse.of(3, "BIL450", "Yapay Zeka", List.of("AI"));
    private static final CatalogCourse PHYSICS =
            CatalogCourse.of(4, "FIZ101", "Fizik", List.of());

    private static final List<CatalogCourse> COURSES = List.of(PROGRAMMING_1, PROGRAMMING_2, AI, PHYSICS);

    private final QueryAnalyzer analyzer = new QueryAnalyzer(0.9);

    @Test
    void matchesAliasAsWholeToken() {
        QueryAnalysis analysis = analyzer.analyze("bp2 vize", COURSES);

        assertThat(analysis.courseId()).isEqualTo(2L);
        assertThat(analysis.match()).isEqualTo(QueryAnalysis.Match.ALIAS);
        assertThat(analysis.expand("bp2 vize")).isEqualTo("bp2 vize Bilgisayar Programlama II BIL201");
    }

    @Test
    void doesNotMatchAliasInsideAnotherWord() {
        QueryAnalysis analysis = analyzer.analyze("send me the email about said exam", COURSES);

        assertThat(analysis.hasCourse()).isFalse();
    }

    @Test
    void matchesAliasWrittenWithSpacesOrRomanNumerals() {
        assertThat(analyzer.analyze("BP 2 final", COURSES).courseId()).isEqualTo(2L);
        assertThat(analyzer.analyze("bp ii final", COURSES).courseId()).isEqualTo(2L);
        assertThat(analyzer.analyze("bp-1 notlar", COURSES).courseId()).isEqualTo(1L);
    }

    @Test
    void prefersTheLongestAlias() {
        CatalogCourse lab = CatalogCourse.of(5, "BIL202", "Programlama Laboratuvari", List.of("BP2 Lab"));
        CatalogCourse lecture = CatalogCourse.of(6, "BIL201", "Bilgisayar Programlama II", List.of("BP2"));

        QueryAnalysis analysis = analyzer.analyze("bp2 lab odevi", List.of(lecture, lab));

        assertThat(analysis.courseId()).isEqualTo(5L);
    }

    @Test
    void matchesCourseCode() {
        QueryAnalysis analysis = analyzer.analyze("bil 450 final", COURSES);

        assertThat(analysis.courseId()).isEqualTo(3L);
        assertThat(analysis.match()).isEqualTo(QueryAnalysis.Match.CODE);
    }

    @Test
    void matchesCourseNameWithTypos() {
        QueryAnalysis analysis = analyzer.analyze("bilgisayr programlma 2 vize", COURSES);

        assertThat(analysis.courseId()).isEqualTo(2L);
        assertThat(analysis.match()).isEqualTo(QueryAnalysis.Match.NAME);
    }

    @Test
    void usesNumbersToPickBetweenSimilarCourses() {
        assertThat(analyzer.analyze("programlama 1 quiz", COURSES).courseId()).isEqualTo(1L);
        assertThat(analyzer.analyze("programlama II quiz", COURSES).courseId()).isEqualTo(2L);
    }

    @Test
    void matchesTurkishNameWrittenWithoutTurkishLetters() {
        CatalogCourse structures = CatalogCourse.of(7, "BIL301", "Veri Yapıları", List.of());

        assertThat(analyzer.analyze("veri yapilari final", List.of(structures)).courseId()).isEqualTo(7L);
    }

    @Test
    void ignoresUnrelatedQueries() {
        assertThat(analyzer.analyze("matematik final 2024", COURSES).hasCourse()).isFalse();
        assertThat(analyzer.analyze("vize", COURSES).hasCourse()).isFalse();
        assertThat(analyzer.analyze("", COURSES).hasCourse()).isFalse();
    }
}
