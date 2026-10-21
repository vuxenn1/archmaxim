package com.archmaxim.search;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Finds the course a query refers to, in this order: an alias ("BP2"), the course
 * code ("BIL201"), or the course name with typos allowed ("programlma 2").
 */
public class QueryAnalyzer {

    private static final int MAX_JOINED_TOKENS = 3;
    private static final int MIN_WORD_LENGTH = 3;
    private static final double MIN_NAME_COVERAGE = 0.5;
    private static final double NUMBER_ADJUSTMENT = 0.1;

    private final JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
    private final double fuzzyThreshold;

    public QueryAnalyzer(double fuzzyThreshold) {
        this.fuzzyThreshold = fuzzyThreshold;
    }

    public QueryAnalysis analyze(String query, List<CatalogCourse> courses) {
        List<String> tokens = TextNormalizer.tokens(query);
        if (tokens.isEmpty() || courses.isEmpty()) {
            return QueryAnalysis.NONE;
        }
        Set<String> joined = joinedWindows(tokens);

        return matchAlias(tokens, joined, courses)
                .map(course -> QueryAnalysis.of(course, QueryAnalysis.Match.ALIAS))
                .or(() -> matchCode(joined, courses).map(course -> QueryAnalysis.of(course, QueryAnalysis.Match.CODE)))
                .or(() -> matchName(tokens, courses).map(course -> QueryAnalysis.of(course, QueryAnalysis.Match.NAME)))
                .orElse(QueryAnalysis.NONE);
    }

    // The longest alias wins, so "bp2 lab" prefers an alias "bp2 lab" over "bp2".
    private Optional<CatalogCourse> matchAlias(List<String> tokens, Set<String> joined, List<CatalogCourse> courses) {
        CatalogCourse best = null;
        int bestLength = 0;
        for (CatalogCourse course : courses) {
            for (CatalogCourse.Alias alias : course.aliases()) {
                boolean matches = containsSequence(tokens, alias.tokens()) || joined.contains(alias.compact());
                if (matches && alias.compact().length() > bestLength) {
                    best = course;
                    bestLength = alias.compact().length();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private Optional<CatalogCourse> matchCode(Set<String> joined, List<CatalogCourse> courses) {
        return courses.stream()
                .filter(course -> !course.compactCode().isEmpty() && joined.contains(course.compactCode()))
                .max(Comparator.comparingInt(course -> course.compactCode().length()));
    }

    /**
     * Scores a course by the share of its significant name words that appear in
     * the query, typos allowed. Numbers only adjust the score, so "programlama 2"
     * prefers "Programlama II" over "Programlama I" without numbers deciding alone.
     */
    private Optional<CatalogCourse> matchName(List<String> tokens, List<CatalogCourse> courses) {
        List<String> queryWords = tokens.stream().filter(QueryAnalyzer::isWord).toList();
        if (queryWords.isEmpty()) {
            return Optional.empty();
        }
        Set<String> queryNumbers = numbers(tokens);

        CatalogCourse best = null;
        double bestScore = 0;
        for (CatalogCourse course : courses) {
            List<String> nameWords = course.nameTokens().stream().filter(QueryAnalyzer::isWord).toList();
            if (nameWords.isEmpty()) {
                continue;
            }
            long matched = nameWords.stream().filter(word -> matchesAny(word, queryWords)).count();
            double coverage = (double) matched / nameWords.size();
            if (coverage < MIN_NAME_COVERAGE) {
                continue;
            }
            double score = coverage + numberAdjustment(numbers(course.nameTokens()), queryNumbers);
            if (score > bestScore) {
                best = course;
                bestScore = score;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean matchesAny(String word, List<String> candidates) {
        for (String candidate : candidates) {
            if (similarity.apply(word, candidate) >= fuzzyThreshold) {
                return true;
            }
        }
        return false;
    }

    private static double numberAdjustment(Set<String> courseNumbers, Set<String> queryNumbers) {
        if (courseNumbers.isEmpty() || queryNumbers.isEmpty()) {
            return 0;
        }
        for (String number : queryNumbers) {
            if (courseNumbers.contains(number)) {
                return NUMBER_ADJUSTMENT;
            }
        }
        return -NUMBER_ADJUSTMENT;
    }

    private static Set<String> joinedWindows(List<String> tokens) {
        Set<String> windows = new HashSet<>();
        for (int start = 0; start < tokens.size(); start++) {
            StringBuilder window = new StringBuilder();
            for (int end = start; end < Math.min(tokens.size(), start + MAX_JOINED_TOKENS); end++) {
                window.append(tokens.get(end));
                windows.add(window.toString());
            }
        }
        return windows;
    }

    private static boolean containsSequence(List<String> tokens, List<String> sequence) {
        if (sequence.isEmpty() || sequence.size() > tokens.size()) {
            return false;
        }
        for (int start = 0; start + sequence.size() <= tokens.size(); start++) {
            if (tokens.subList(start, start + sequence.size()).equals(sequence)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWord(String token) {
        return token.length() >= MIN_WORD_LENGTH && !isNumber(token);
    }

    private static boolean isNumber(String token) {
        return token.chars().allMatch(Character::isDigit);
    }

    private static Set<String> numbers(List<String> tokens) {
        Set<String> numbers = new HashSet<>();
        for (String token : tokens) {
            if (isNumber(token)) {
                numbers.add(token);
            }
        }
        return numbers;
    }
}
