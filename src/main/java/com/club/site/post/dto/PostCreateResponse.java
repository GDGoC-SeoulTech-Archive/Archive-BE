package com.club.site.post.dto;

public record PostCreateResponse(
        String postId,
        PostDetailResponse.PostDetail post
) {
}

