package com.club.site.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MeBootstrapRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[0-9]+ê¸?", message = "generation must match Nê¸?(e.g. 1ê¸?") String generation,
        @NotBlank String part
) {
}


