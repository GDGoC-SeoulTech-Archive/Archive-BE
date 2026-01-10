package com.club.site.comment.dto;

public record CommentListItemDTO(
        String commentId,
        String postId,
        String body,
        String authorId,
        String authorName,
        String createdAt,
        String updatedAt
) {
}

