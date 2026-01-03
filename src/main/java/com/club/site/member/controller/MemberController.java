package com.club.site.member.controller;

import com.club.site.member.dto.MemberDTO;
import com.club.site.member.dto.MemberListResponse;
import com.club.site.member.service.MemberService;
import com.club.site.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j // 로그용입니다
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // React에서 요청 허용
public class MemberController {

    private final MemberService memberService;

    // 멤버 Mock Data 초기화용
    // POST http://localhost:8080/api/v1/members/init
    @PostMapping("/init")
    public ApiResponse<String> initData() {
        String result = memberService.saveMockData();
        return ApiResponse.ok(result);
    }

    // 회원가입 로직 추가
    @PostMapping("/signup")
    public ApiResponse<String> signup(@RequestBody MemberDTO memberDTO) {
        log.info("🚀 회원가입 요청 도착! UID: {}, 이름: {}, Part: {}",
                memberDTO.uid(), memberDTO.name(), memberDTO.part());
        String result = memberService.signUp(memberDTO);
        return ApiResponse.ok(result);
    }

    @GetMapping("/me")
    public ApiResponse<MemberDTO> getMyProfile() {
        // (나중에 memberService.getMyProfile()로 교체 예정)
        return ApiResponse.ok(null);
    }

    /**
     * 멤버 리스트 조회 (필터 + 페이지네이션)
     * GET http://localhost:8080/api/v1/members?generation=5기&part=WEB-FE...
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
        return ApiResponse.ok(response);
    }

    /**
     * 멤버 상세 조회 (공개)
     * GET /api/v1/members/{uid}
     */
    @GetMapping("/{uid}")
    public ApiResponse<MemberDTO> getMemberDetail(@PathVariable String uid) {
        MemberDTO member = memberService.getMemberByUid(uid);
        return ApiResponse.ok(member);
    }
}