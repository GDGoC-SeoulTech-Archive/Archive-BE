package com.club.site.skill.dto;

import jakarta.validation.constraints.NotNull;

public record SkillPatchRequest(@NotNull Boolean active) {
}


