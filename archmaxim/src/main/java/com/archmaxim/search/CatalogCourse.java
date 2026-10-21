package com.archmaxim.search;

import java.util.List;

/**
 * A course as search sees it: normalized tokens of its name, code and aliases.
 */
public record CatalogCourse(long id, String code, String name,
                            String compactCode, List<String> nameTokens, List<Alias> aliases) {

    public record Alias(String value, List<String> tokens, String compact) {

        static Alias of(String value) {
            List<String> tokens = TextNormalizer.tokens(value);
            return new Alias(value, tokens, String.join("", tokens));
        }
    }

    public static CatalogCourse of(long id, String code, String name, List<String> aliases) {
        return new CatalogCourse(id, code, name,
                String.join("", TextNormalizer.tokens(code)),
                TextNormalizer.tokens(name),
                aliases.stream().map(Alias::of).filter(alias -> !alias.tokens().isEmpty()).toList());
    }

    public String expansion() {
        return name + " " + code;
    }
}
