package com.archmaxim.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

public record FileDownload(Resource resource, String filename, MediaType contentType, long size) {

    // The UTF-8 form keeps Turkish characters intact and cannot be broken by quotes in the name.
    public ContentDisposition contentDisposition() {
        return ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
    }
}
