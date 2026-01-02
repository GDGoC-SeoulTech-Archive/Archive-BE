package com.club.site.dto.post;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PostUpdateRequest(
        @NotBlank String title,
        @NotBlank String body,
        @NotBlank String eventDate,
        List<String> imageUrls
) {
}


