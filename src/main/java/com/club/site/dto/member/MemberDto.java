package com.club.site.dto.member;

import com.club.site.model.MemberStatus;
import com.club.site.model.Part;
import com.club.site.model.SocialLink;

import java.util.List;

public record MemberDto(
        String uid,
        String name,
        String generation,
        Part part,
        String bio,
        List<SocialLink> socialLinks,
        List<String> skillIds,
        GithubDto github,
        MemberStatus status,
        String createdAt,
        String updatedAt
) {
}


