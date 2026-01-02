package com.club.site.dto.member;

public record MeBootstrapResponse(
        MemberDTO member,
        boolean isProfileComplete
) {
}


