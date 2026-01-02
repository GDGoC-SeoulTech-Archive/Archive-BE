package com.club.site.dto.skill;

import jakarta.validation.constraints.NotBlank;

public record SkillCreateRequest(@NotBlank String displayName) {
}


