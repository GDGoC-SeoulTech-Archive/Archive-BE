package com.club.site.member.dto;

import com.google.cloud.Timestamp; // Firebase Timestamp
import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDTO {
    private String uid;
    private String name;
    private String generation;
    private String part;
    private String status;
    private String bio;

    private String photoUrl;

    private List<SocialLink> socialLinks;
    private List<String> skillIds;

    private GithubInfo github;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String introduction;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLink {
        private String type;
        private String url;
    }

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GithubInfo {
        private String username;
    }
}

