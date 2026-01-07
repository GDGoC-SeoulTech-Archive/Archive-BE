package com.club.site.comment.dto;

public record CommentDetail(
        String commentId,
        String postId,
        String body,
        String authorId,
        String authorName,
        String createdAt,
        String updatedAt
) {
    /**
     * CommentDto를 CommentDetail로 변환
     */
    public static CommentDetail from(CommentDto commentDto) {
        return new CommentDetail(
                commentDto.commentId(),
                commentDto.postId(),
                commentDto.body(),
                commentDto.authorId(),
                commentDto.authorName(),
                commentDto.createdAt(),
                commentDto.updatedAt()
        );
    }
}

