package com.club.site.controller;

import com.club.site.dto.member.MeBootstrapRequest;
import com.club.site.dto.member.MeBootstrapResponse;
import com.club.site.dto.member.MeResponse;
import com.club.site.dto.member.MeUpdateRequest;
import com.club.site.model.Part;
import com.club.site.security.FirebasePrincipal;
import com.club.site.service.MemberService;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final MemberService memberService;

    public MeController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/bootstrap")
    public ApiResponse<MeBootstrapResponse> bootstrap(Authentication authentication, @Valid @RequestBody MeBootstrapRequest request) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        Part part = Part.parse(request.part());

        var member = memberService.bootstrap(
                principal.uid(),
                request.name(),
                request.generation(),
                part,
                principal.photoUrl(),
                null
        );

        boolean isProfileComplete = member.name() != null && !member.name().isBlank()
                && member.generation() != null && !member.generation().isBlank()
                && member.part() != null;

        return ApiResponse.ok(new MeBootstrapResponse(member, isProfileComplete));
    }

    @GetMapping
    public ApiResponse<MeResponse> me(Authentication authentication) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        return ApiResponse.ok(new MeResponse(memberService.getMe(principal.uid())));
    }

    @PutMapping
    public ApiResponse<MeResponse> update(Authentication authentication, @Valid @RequestBody MeUpdateRequest request) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        return ApiResponse.ok(new MeResponse(
                memberService.updateMe(principal.uid(), request.bio(), request.socialLinks(), request.skillIds())
        ));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(Authentication authentication) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        memberService.anonymizeMe(principal.uid());
        return ApiResponse.ok();
    }
}


