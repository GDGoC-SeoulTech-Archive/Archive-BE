package com.club.site.controller;

import com.club.site.dto.PagedResult;
import com.club.site.dto.member.MemberDTO; // Record
import com.club.site.security.FirebasePrincipal;
import com.club.site.service.MemberService;
import com.club.site.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class MembersController {

    private final MemberService memberService;

    @PostMapping("/init")
    public ApiResponse<String> initData() {
        String result = memberService.saveMockData();
        return ApiResponse.ok(result);
    }

    @GetMapping("/me")
    public ApiResponse<MemberDTO> getMe(@AuthenticationPrincipal FirebasePrincipal principal) throws Exception {
        return ApiResponse.ok(memberService.getMe(principal.uid()));
    }

    @GetMapping
    public ApiResponse<PagedResult<MemberDTO>> list(
            @RequestParam(required = false) String generation,
            @RequestParam(required = false) String part,
            @RequestParam(required = false) List<String> skillIds,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) throws Exception {
        return ApiResponse.ok(memberService.listMembers(generation, part, skillIds, pageSize, cursor));
    }

    @GetMapping("/{uid}")
    public ApiResponse<MemberDTO> get(@PathVariable String uid) throws Exception {
        return ApiResponse.ok(memberService.getPublicMember(uid));
    }
}