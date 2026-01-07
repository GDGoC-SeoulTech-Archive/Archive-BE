package com.club.site.comment.dto;

public record CommentUpdateResponse(
        String commentId,
        CommentDetail comment
) {
}

