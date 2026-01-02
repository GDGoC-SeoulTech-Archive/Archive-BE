package com.club.site.dto.member;

import jakarta.validation.constraints.NotBlank;

public record SocialLinkRequest(
        @NotBlank String type,
        @NotBlank String url
) {
}


