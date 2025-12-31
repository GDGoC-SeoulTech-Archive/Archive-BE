package com.club.site.common.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // 빌더 패턴 추가 (나중에 객체 만들 때 엄청 편해요)
public class MemberDTO {
    private String uid;
    private String name;
    private String generation;
    private String part;       // 나중에 Enum으로 바꾸기

    private List<SocialLink> socialLinks;
    private List<String> skillIds;
    private GithubInfo github;
    private String bio;           // 한 줄 소개
    private String introduction;  // 상세 자기소개
    private String status;    // ACTIVE 또는 ANONYMIZED
    private String createdAt;
    private String updatedAt;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLink {
        private String type; // GITHUB, BLOG...
        private String url;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GithubInfo {
        private String username;
        private String photoUrl;
    }
}