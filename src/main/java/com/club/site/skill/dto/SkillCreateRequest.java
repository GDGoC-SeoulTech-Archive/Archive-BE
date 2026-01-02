package com.club.site.skill.dto;

import jakarta.validation.constraints.NotBlank;

public record SkillCreateRequest(@NotBlank String displayName) {
}


