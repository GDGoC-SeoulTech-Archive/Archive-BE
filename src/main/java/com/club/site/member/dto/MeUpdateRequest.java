package com.club.site.member.dto;

import java.util.List;

public record MeUpdateRequest(
        String bio,
        List<SocialLinkRequest> socialLinks,
        List<String> skillIds
) {}