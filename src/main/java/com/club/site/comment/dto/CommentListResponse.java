package com.club.site.comment.dto;

import java.util.List;

public record CommentListResponse(
        List<CommentListItemDTO> items,
        String nextCursor
) {
}

