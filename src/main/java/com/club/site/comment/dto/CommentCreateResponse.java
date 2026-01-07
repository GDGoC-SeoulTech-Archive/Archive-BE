package com.club.site.comment.dto;

public record CommentCreateResponse(
        String commentId,
        CommentDetail comment
) {
}

