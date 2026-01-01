package com.club.site.member.controller;

import com.club.site.common.response.ApiResponse;
import com.club.site.member.dto.MemberDTO;
import com.club.site.member.dto.MemberListResponse;
import com.club.site.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // React(3000번)에서 요청 허용
public class MemberController {

    private final MemberService memberService;

    // 🔥 [핵심] 이 API를 호출하면 Mock 데이터가 DB에 저장됩니다.
    // POST http://localhost:8080/api/v1/members/init
    @PostMapping("/init")
    public ApiResponse<String> initData() {
        String result = memberService.saveMockData();
        return ApiResponse.success(result);
    }

    @GetMapping("/me")
    public ApiResponse<MemberDTO> getMyProfile() {
        // (나중에 memberService.getMyProfile()로 교체 예정)
        return ApiResponse.success(null);
    }

    /**
     * 멤버 리스트 조회 (필터 + 페이지네이션)
     * GET /api/v1/members?generation=5기&part=WEB-FE&skillIds=React&skillIds=Vue&pageSize=20&cursor=...
     */
    @GetMapping
    public ApiResponse<MemberListResponse> getAllMembers(
            @RequestParam(required = false) String generation,
            @RequestParam(required = false) String part,
            @RequestParam(required = false) List<String> skillIds,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        MemberListResponse response = memberService.getMembers(generation, part, skillIds, pageSize, cursor);
        return ApiResponse.success(response);
    }

    /**
     * 멤버 상세 조회 (공개)
     * GET /api/v1/members/{uid}
     */
    @GetMapping("/{uid}")
    public ApiResponse<MemberDTO> getMemberDetail(@PathVariable String uid) {
        MemberDTO member = memberService.getMemberByUid(uid);
        return ApiResponse.success(member);
    }
}
