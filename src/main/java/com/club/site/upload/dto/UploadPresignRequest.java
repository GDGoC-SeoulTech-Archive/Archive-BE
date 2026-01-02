package com.club.site.upload.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadPresignRequest(
        @NotBlank String contentType,
        @NotBlank String fileName
) {
}


