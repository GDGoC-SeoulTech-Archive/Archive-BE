package com.club.site.post.dto;

public record PostListItemDTO(
        String postId,
        String title,
        String eventDate, // YYYY-MM-DD | null
        String thumbnailUrl,
        String authorId,
        String authorName,
        String createdAt,
        String updatedAt
) {
}

