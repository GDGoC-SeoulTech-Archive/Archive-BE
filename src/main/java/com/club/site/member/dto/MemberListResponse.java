package com.club.site.member.dto;

import java.util.List;

public record MemberListResponse(
        List<MemberListItemDTO> items,
        String nextCursor
) {
}

