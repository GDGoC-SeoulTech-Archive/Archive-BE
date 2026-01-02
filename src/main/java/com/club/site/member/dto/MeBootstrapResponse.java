package com.club.site.member.dto;

public record MeBootstrapResponse(
        MemberDTO member,
        boolean isProfileComplete
) {
}


