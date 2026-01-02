package com.club.site.member.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLinkRequest(
        @NotBlank String type,
        @NotBlank String url
) {
}


