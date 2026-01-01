package com.club.site.controller;

import com.club.site.dto.PagedResult;
import com.club.site.dto.member.MemberDto;
import com.club.site.service.MemberService;
import com.club.site.web.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
public class MembersController {
    private final MemberService memberService;

    public MembersController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResponse<PagedResult<MemberDto>> list(
            @RequestParam(required = false) String generation,
            @RequestParam(required = false) String part,
            @RequestParam(required = false) List<String> skillIds,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) throws Exception {
        return ApiResponse.ok(memberService.listMembers(generation, part, skillIds, pageSize, cursor));
    }

    @GetMapping("/{uid}")
    public ApiResponse<MemberDto> get(@PathVariable String uid) throws Exception {
        return ApiResponse.ok(memberService.getPublicMember(uid));
    }
}


