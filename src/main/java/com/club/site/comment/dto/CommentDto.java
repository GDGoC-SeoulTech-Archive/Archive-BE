package com.club.site.comment.dto;

public record CommentDto(
        String commentId,
        String postId,
        String body,              // HTML 이스케이프된 텍스트
        String authorId,
        String authorName,
        String createdAt,
        String updatedAt
) {
}

