package com.club.site.dto.skill;

public record SkillDto(
        String id,
        String displayName,
        String normalized,
        boolean active,
        String createdAt,
        String updatedAt
) {
}


