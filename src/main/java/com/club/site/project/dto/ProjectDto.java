package com.club.site.project.dto;

import java.util.List;

public record ProjectDto(
        String id,
        String title,
        String body,
        String startDate,
        List<ImageInfo> images,
        String thumbnailUrl,
        List<String> skills,
        List<String> members,
        String authorId,
        String authorName,
        String createdAt,
        String updatedAt
) {
}
