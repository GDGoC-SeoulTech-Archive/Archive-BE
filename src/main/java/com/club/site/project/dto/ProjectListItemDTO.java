package com.club.site.project.dto;

import java.util.List;

public record ProjectListItemDTO(
        String projectId,
        String title,
        String startDate,
        String thumbnailUrl,
        List<String> skills,
        String authorId,
        String authorName,
        String createdAt,
        String updatedAt
) {
}
