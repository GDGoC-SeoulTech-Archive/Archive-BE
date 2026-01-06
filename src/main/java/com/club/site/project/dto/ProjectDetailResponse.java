package com.club.site.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProjectDetailResponse(
        ProjectDetail project
) {
    public record ProjectDetail(
            @JsonProperty("projectId") String projectId,
            String title,
            String body,
            String startDate,
            List<ImageInfo> images,
            String thumbnailUrl,
            List<String> skills,
            List<String> members,
            String authorId,
            String authorName,
            String createdAt,
            String updatedAt
    ) {
        public static ProjectDetail from(ProjectDto projectDto) {
            return new ProjectDetail(
                    projectDto.id(),
                    projectDto.title(),
                    projectDto.body(),
                    projectDto.startDate(),
                    projectDto.images(),
                    projectDto.thumbnailUrl(),
                    projectDto.skills(),
                    projectDto.members(),
                    projectDto.authorId(),
                    projectDto.authorName(),
                    projectDto.createdAt(),
                    projectDto.updatedAt()
            );
        }
    }
}
