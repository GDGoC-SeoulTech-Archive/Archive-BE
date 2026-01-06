package com.club.site.member.controller;

 // TODO: feature/member-list 브랜치 형식으로 변경 중 - MeController는 나중에 처리
 import com.club.site.member.dto.MeBootstrapRequest;
 import com.club.site.member.dto.MeBootstrapResponse;
 import com.club.site.member.dto.MeUpdateRequest;
 import com.club.site.member.dto.MemberDTO;
 import com.club.site.member.service.MemberService;
 import com.club.site.model.Part;
 import com.club.site.auth.dto.FirebasePrincipal;
 import com.club.site.web.ApiResponse;
 import jakarta.validation.Valid;
 import lombok.RequiredArgsConstructor;
 import org.springframework.security.core.Authentication;
 import org.springframework.web.bind.annotation.*;

 @RestController
 @RequestMapping("/api/v1/me")
 @RequiredArgsConstructor
 public class MeController {

     private final MemberService memberService;

     @PostMapping("/bootstrap")
     public ApiResponse<MeBootstrapResponse> bootstrap(
             Authentication authentication,
             @Valid @RequestBody MeBootstrapRequest request // 여기서 Request 받음
     ) {
         FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();

         // Service로 넘김 (UID, PhotoUrl은 토큰에서, 나머지는 Request에서)
         MeBootstrapResponse response = memberService.bootstrap(
                 principal.uid(),
                 request,
                 principal.photoUrl()
         );

         return ApiResponse.ok(response);
     }

     @GetMapping
     public ApiResponse<MemberDTO> me(Authentication authentication) throws Exception {
         FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
         return ApiResponse.ok(memberService.getMemberByUid(principal.uid()));
     }

     @PutMapping
     public ApiResponse<MemberDTO> update(Authentication authentication, @Valid @RequestBody MeUpdateRequest request) throws Exception {
         FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
         return ApiResponse.ok(memberService.updateMe(
                 principal.uid(),
                 request.bio(),
                 request.socialLinks(),
                 request.skillIds()
         ));
     }

     @DeleteMapping
     public ApiResponse<Void> delete(Authentication authentication) throws Exception {
         FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
         memberService.anonymizeMember(principal.uid());
         return ApiResponse.ok();
     }
 }