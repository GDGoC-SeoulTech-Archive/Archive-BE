package com.club.site.dto.skill;

import jakarta.validation.constraints.NotNull;

public record SkillPatchRequest(@NotNull Boolean active) {
}


