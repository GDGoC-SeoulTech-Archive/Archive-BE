package com.club.site.dto.member;

import com.club.site.model.MemberStatus;
import com.club.site.model.Part;
import com.club.site.model.SocialLink;
import java.util.List;

// class가 아니라 record입니다!
public record MemberDTO(
        String uid,
        String name,
        // 어차피 string이니 'n기' 로 저장
        String generation,
        Part part,
        String bio,
        List<SocialLink> socialLinks,
        List<String> skillIds,
        GithubDTO github,
        MemberStatus status,
        String createdAt, // Service에서 String으로 변환해서 줍니다
        String updatedAt
) {
}