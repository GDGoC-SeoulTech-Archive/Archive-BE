package com.club.site.common.controller;

import com.club.site.common.response.ApiResponse;
import com.club.site.common.dto.MemberDTO; // 👈 DTO 임포트 필수!
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    // 1. 내 정보 조회 (GET /api/v1/members/me)
    @GetMapping("/me")
    public ApiResponse<MemberDTO> getMyProfile() {
        MemberDTO mockMe = MemberDTO.builder()
                .uid("my-uid-123")
                .name("염정우")
                .generation("5기")
                .part("App") // 프론트 뱃지 테스트용: App, WEB-BE, AI 등으로 바꿔보세요
                .status("ACTIVE")
                .bio("5기 Organizer") // 짧은 소개 (카드용)
                .introduction("안녕하세요! 염정우입니다.\n개발왕이 되고싶습니다.") // 상세 소개 (모달용)
                .github(new MemberDTO.GithubInfo("yeomine", "https://avatars.githubusercontent.com/u/yeomnin")) // 실제 본인 깃허브 ID 넣으면 사진 뜸
                .skillIds(List.of("Spring", "Vue", "React"))
                .socialLinks(List.of(
                        new MemberDTO.SocialLink("BLOG", "https://velog.io/@yjw326/posts")
                ))
                .build();

        return ApiResponse.success(mockMe);
    }

    // 2. 전체 멤버 조회 (GET /api/v1/members)
    @GetMapping
    public ApiResponse<List<MemberDTO>> getAllMembers() {
        List<MemberDTO> mockList = new ArrayList<>();

        // 멤버 1 (정우 - App)
        mockList.add(MemberDTO.builder()
                .uid("1")
                .name("염정우")
                .part("App")
                .generation("5기")
                .status("ACTIVE")
                .bio("GDGOC가 터지면 제 탓입니다.")
                .introduction("백엔드 개발을 주로 담당하고 있습니다. 마이크로서비스 아키텍처에 관심이 많아요.")
                .github(new MemberDTO.GithubInfo("yeomine", "https://github.com/yeomine.png"))
                .socialLinks(List.of(new MemberDTO.SocialLink("BLOG", "https://velog.io/@yjw326/posts")))
                .build());

        // 멤버 2 (가연 - WEB-FE)
        mockList.add(MemberDTO.builder()
                .uid("2")
                .name("이가연")
                .part("WEB-FE")
                .generation("5기")
                .status("ACTIVE")
                .bio("프론트엔드 깎는 장인")
                .introduction("React와 Next.js를 사용하여 사용자 경험을 개선하는 것을 좋아합니다.")
                .skillIds(List.of("React", "TypeScript", "Tailwind"))
                .build());

        // 멤버 3 (대훈 - AI)
        mockList.add(MemberDTO.builder()
                .uid("3")
                .name("권대훈")
                .part("AI")
                .generation("5기")
                .status("ACTIVE")
                .bio("AI가 세상을 지배한다")
                .introduction("LLM 모델 파인튜닝과 RAG 시스템 구축을 연구하고 있습니다.")
                .build());

        // 멤버 4 (민석 - Design)
        mockList.add(MemberDTO.builder()
                .uid("4")
                .name("최민석")
                .part("Design")
                .generation("5기")
                .status("ACTIVE")
                .bio("하이하이~~")
                .introduction("피그마장인 최민석입니다.")
                .build());

        // 멤버 5 (채영 - BE)
        mockList.add(MemberDTO.builder()
                .uid("5")
                .name("임채영")
                .part("WEB-BE")
                .generation("5기")
                .status("ACTIVE")
                .bio("서버 짓는 여인")
                .introduction("어 나 5기 백엔드 임채영인데")
                .build());

        return ApiResponse.success(mockList);
    }

    // 3. 특정 멤버 상세 조회 (GET /api/v1/members/{uid})
    @GetMapping("/{uid}")
    public ApiResponse<MemberDTO> getMemberDetail(@PathVariable String uid) {
        // 실제로는 DB에서 조회해야 하지만, 지금은 요청받은 UID로 가짜 객체 생성
        MemberDTO mockMember = MemberDTO.builder()
                .uid(uid)
                .name("검색된 멤버")
                .part("Design") // 디자인 파트 테스트
                .generation("2기")
                .status("ACTIVE")
                .bio("이것은 " + uid + "님의 상세 정보입니다.")
                .introduction("상세 페이지 테스트를 위한 더미 데이터입니다. DB가 연결되면 실제 데이터가 나옵니다.")
                .skillIds(List.of("Figma", "Photoshop", "Illustrator"))
                .build();

        return ApiResponse.success(mockMember);
    }
}