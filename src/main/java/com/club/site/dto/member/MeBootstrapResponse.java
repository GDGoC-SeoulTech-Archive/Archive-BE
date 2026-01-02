package com.club.site.dto.member;

public record MeBootstrapResponse(
        MemberDto member,
        boolean isProfileComplete
) {
}


