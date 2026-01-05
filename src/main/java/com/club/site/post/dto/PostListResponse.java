package com.club.site.post.dto;

import java.util.List;

public record PostListResponse(
        List<PostListItemDTO> items,
        String nextCursor
) {
}

