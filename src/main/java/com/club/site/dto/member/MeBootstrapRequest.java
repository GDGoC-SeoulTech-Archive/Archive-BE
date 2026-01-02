package com.club.site.dto.member;

import jakarta.validation.constraints.NotBlank;

public record MeBootstrapRequest(
        @NotBlank String name,
        // 어차피 string이니 'n기' 로 저장
        @NotBlank String generation,
        @NotBlank String part
) {}