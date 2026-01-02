package com.club.site.dto.upload;

import jakarta.validation.constraints.NotBlank;

public record UploadPresignRequest(
        @NotBlank String contentType,
        @NotBlank String fileName
) {
}


