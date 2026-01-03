package com.club.site.member.dto;

import com.club.site.model.MemberStatus;
import com.club.site.model.Part;

import java.util.List;

public record MemberListItemDTO(
        String uid,
        String name,
        String generation,
        Part part,
        List<String> skillIds,
        GithubDTO github,
        String bioShort,
        SocialSummary socialSummary,
        MemberStatus status
) {
}

