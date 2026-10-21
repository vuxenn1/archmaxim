package com.archmaxim.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void foldsTurkishLettersIndependentOfLocale() {
        assertThat(TextNormalizer.normalize("BİL201")).isEqualTo("bil201");
        assertThat(TextNormalizer.normalize("BIL201")).isEqualTo("bil201");
        assertThat(TextNormalizer.normalize("bıl201")).isEqualTo("bil201");
        assertThat(TextNormalizer.normalize("Şişli Güz Dönemi Çalışması Ödev"))
                .isEqualTo("sisli guz donemi calismasi odev");
        assertThat(TextNormalizer.normalize("ĞÜŞİÖÇ ğüşıöç")).isEqualTo("gusioc gusioc");
    }

    @Test
    void writesStandaloneRomanNumeralsAsDigits() {
        assertThat(TextNormalizer.tokens("Bilgisayar Programlama II")).containsExactly("bilgisayar", "programlama", "2");
        assertThat(TextNormalizer.tokens("Fizik I")).containsExactly("fizik", "1");
        assertThat(TextNormalizer.tokens("Matematik IV vize")).containsExactly("matematik", "4", "vize");
    }

    @Test
    void leavesNumeralLettersInsideWordsAlone() {
        assertThat(TextNormalizer.tokens("vize xi civic")).containsExactly("vize", "xi", "civic");
    }

    @Test
    void turnsPunctuationIntoSeparators() {
        assertThat(TextNormalizer.tokens("BP-2, (vize)!")).containsExactly("bp", "2", "vize");
        assertThat(TextNormalizer.normalize("  a__b..c  ")).isEqualTo("a b c");
    }

    @Test
    void handlesEmptyInput() {
        assertThat(TextNormalizer.tokens(null)).isEmpty();
        assertThat(TextNormalizer.tokens("  ")).isEmpty();
        assertThat(TextNormalizer.normalize("!!!")).isEmpty();
    }
}
