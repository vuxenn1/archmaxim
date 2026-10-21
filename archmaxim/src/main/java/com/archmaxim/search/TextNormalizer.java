package com.archmaxim.search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Brings course names, codes, aliases and queries into one comparable form:
 * Turkish and other accented letters folded to ASCII, lowercase, punctuation
 * removed, and standalone roman numerals written as digits.
 *
 * <p>Lowercasing uses {@link Locale#ROOT}, so the result does not depend on the
 * machine's locale. With a Turkish default locale, "BIL201".toLowerCase() would
 * otherwise produce "bıl201".
 */
public final class TextNormalizer {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Map<String, String> ROMAN_NUMERALS = Map.of(
            "i", "1", "ii", "2", "iii", "3", "iv", "4", "v", "5",
            "vi", "6", "vii", "7", "viii", "8", "ix", "9", "x", "10");

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        return String.join(" ", tokens(text));
    }

    /**
     * The normalized text without spaces. Aliases are compared this way, since
     * search treats "BP2" and "bp 2" as the same alias.
     */
    public static String compact(String text) {
        return String.join("", tokens(text));
    }

    public static List<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        // Dotless i has no decomposition, so NFD alone would leave it untouched.
        String folded = text.replace('ı', 'i').replace('İ', 'I');
        folded = Normalizer.normalize(folded, Normalizer.Form.NFD);
        folded = COMBINING_MARKS.matcher(folded).replaceAll("");
        folded = folded.toLowerCase(Locale.ROOT);
        folded = SEPARATORS.matcher(folded).replaceAll(" ").trim();
        if (folded.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(folded.split(" "))
                .map(token -> ROMAN_NUMERALS.getOrDefault(token, token))
                .toList();
    }
}
