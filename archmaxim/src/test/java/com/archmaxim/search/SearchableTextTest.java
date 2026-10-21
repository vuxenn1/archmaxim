package com.archmaxim.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchableTextTest {

    @Test
    void turnsFilenameIntoWords() {
        assertThat(SearchableText.filenameWords("bp2_vize-2025.final.pdf")).isEqualTo("bp2 vize 2025 final");
        assertThat(SearchableText.filenameWords("notlar")).isEqualTo("notlar");
        assertThat(SearchableText.filenameWords(".gitignore")).isEqualTo("gitignore");
    }
}
