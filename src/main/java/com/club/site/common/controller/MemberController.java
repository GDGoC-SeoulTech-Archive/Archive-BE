package com.club.site.common.controller;

import com.club.site.common.response.ApiResponse;
import com.club.site.common.dto.MemberDTO;
import com.club.site.common.service.MemberService; // 서비스 임포트
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
        // 컨트롤러는 "저장해줘"라고 시키기만 합니다.
        // 데이터가 누군지, 몇 명인지는 Service가 알고 있습니다.
        String result = memberService.saveMockData();
        return ApiResponse.success(result);
    }


    @GetMapping("/me")
    public ApiResponse<MemberDTO> getMyProfile() {
        // (나중에 memberService.getMyProfile()로 교체 예정)
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<MemberDTO>> getAllMembers() {
        List<MemberDTO> members = memberService.getAllMembers();
        return ApiResponse.success(members);
    }

    @GetMapping("/{uid}")
    public ApiResponse<MemberDTO> getMemberDetail(@PathVariable String uid) {
        return ApiResponse.success(null);
    }
}