package com.club.site.post.dto;

public record PostUpdateResponse(
        String postId,
        PostDetailResponse.PostDetail post
) {
}

