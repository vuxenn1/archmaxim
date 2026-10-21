package com.archmaxim.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class FileNamesTest {

    @Test
    void keepsOnlyTheLastPathSegment() {
        assertThat(FileService.cleanFilename("C:\\Users\\ali\\Desktop\\vize.pdf")).isEqualTo("vize.pdf");
        assertThat(FileService.cleanFilename("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileService.cleanFilename("notlar/hafta1.pdf")).isEqualTo("hafta1.pdf");
    }

    @Test
    void dropsControlCharactersAndFallsBackForEmptyNames() {
        assertThat(FileService.cleanFilename("vi\r\nze.pdf")).isEqualTo("vize.pdf");
        assertThat(FileService.cleanFilename(null)).isEqualTo("file");
        assertThat(FileService.cleanFilename("  ")).isEqualTo("file");
        assertThat(FileService.cleanFilename("..")).isEqualTo("file");
    }

    @Test
    void shortensLongNamesButKeepsTheExtension() {
        String cleaned = FileService.cleanFilename("a".repeat(400) + ".pdf");

        assertThat(cleaned).hasSize(255).endsWith(".pdf");
    }

    @Test
    void readsTheExtensionInLowerCase() {
        assertThat(FileService.extensionOf("Vize.PDF")).isEqualTo("pdf");
        assertThat(FileService.extensionOf("archive.tar.gz")).isEqualTo("gz");
        assertThat(FileService.extensionOf("README")).isEmpty();
        assertThat(FileService.extensionOf(".env")).isEmpty();
        assertThat(FileService.extensionOf("file.")).isEmpty();
    }

    @Test
    void contentDispositionKeepsTurkishCharacters() {
        FileDownload download = new FileDownload(new ByteArrayResource(new byte[0]),
                "Vize Soruları \"2025\".pdf", MediaType.APPLICATION_PDF, 0);

        String header = download.contentDisposition().toString();

        assertThat(header).startsWith("attachment;");
        assertThat(header).contains("filename*=UTF-8''Vize%20Sorular%C4%B1%20%222025%22.pdf");
    }
}
