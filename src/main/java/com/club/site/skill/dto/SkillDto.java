package com.club.site.skill.dto;

public record SkillDto(
        String id,
        String displayName,
        String normalized,
        boolean active,
        String createdAt,
        String updatedAt
) {
}


