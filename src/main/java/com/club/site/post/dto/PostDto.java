package com.club.site.post.dto;

import java.util.List;

public record PostDto(
        String id,
        String title,
        String body,
        String eventDate,
        List<ImageInfo> images,  // List<String> → List<ImageInfo> (url + path)
        String thumbnailUrl,
        String authorId,
        String authorName,
        String createdAt,
        String updatedAt
) {
}


