package com.club.site.project.dto;

import java.util.List;

public record ProjectListResponse(
        List<ProjectListItemDTO> items,
        String nextCursor
) {
}
