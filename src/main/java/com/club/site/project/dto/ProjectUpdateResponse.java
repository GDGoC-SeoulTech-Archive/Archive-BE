package com.club.site.project.dto;

public record ProjectUpdateResponse(
        String projectId,
        ProjectDetailResponse.ProjectDetail project
) {
}
