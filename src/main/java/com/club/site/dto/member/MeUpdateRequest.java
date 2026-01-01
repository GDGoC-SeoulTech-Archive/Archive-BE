package com.club.site.dto.member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record MeUpdateRequest(
        String bio,
        @Valid List<SocialLinkRequest> socialLinks,
        List<@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "skillId must be normalized (e.g. spring-boot)") String> skillIds
) {
}


