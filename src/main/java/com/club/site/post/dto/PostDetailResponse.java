package com.club.site.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 게시글 상세 조회 Response DTO
 * PostDto를 래핑하되, id 필드를 postId로 매핑
 */
public record PostDetailResponse(
        PostDetail post
) {
    /**
     * 게시글 상세 정보
     * PostDto의 id를 postId로 매핑
     */
    public record PostDetail(
            @JsonProperty("postId") String postId,
            String title,
            String body,
            String eventDate, // YYYY-MM-DD | null
            List<ImageInfo> images,  // List<String> → List<ImageInfo>
            String thumbnailUrl,
            String authorId,
            String authorName,
            String createdAt,
            String updatedAt
    ) {
        /**
         * PostDto를 PostDetail로 변환
         */
        public static PostDetail from(PostDto postDto) {
            return new PostDetail(
                    postDto.id(), // id를 postId로 매핑
                    postDto.title(),
                    postDto.body(),
                    postDto.eventDate(),
                    postDto.images(),
                    postDto.thumbnailUrl(),
                    postDto.authorId(),
                    postDto.authorName(),
                    postDto.createdAt(),
                    postDto.updatedAt()
            );
        }
    }
}

