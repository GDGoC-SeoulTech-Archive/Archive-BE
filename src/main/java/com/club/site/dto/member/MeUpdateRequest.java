package com.club.site.dto.member;

import java.util.List;

public record MeUpdateRequest(
        String bio,
        List<SocialLinkRequest> socialLinks,
        List<String> skillIds
) {}