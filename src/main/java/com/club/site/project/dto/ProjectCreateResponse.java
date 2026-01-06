package com.club.site.project.dto;

public record ProjectCreateResponse(
        String projectId,
        ProjectDetailResponse.ProjectDetail project
) {
}
