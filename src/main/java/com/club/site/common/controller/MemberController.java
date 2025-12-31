package com.club.site.common.controller;

import com.club.site.common.response.ApiResponse;
import com.club.site.common.dto.MemberDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    // 1. 내 정보 조회 (GET /api/v1/members/me)
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getMyProfile() {
        // mockData
        Map<String, Object> mockMe = Map.of(
                "uid", "my-uid-123",
                "name", "염정우",
                "role", "ORGANIZER",
                "part", "App",
                "generation", "5기"
        );

        return ApiResponse.success(mockMe);
    }

    // 2. 전체 멤버 조회 (GET /api/v1/members)
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getAllMembers() {
        // 멤버 mockData
        Map<String, Object> member1 = Map.of("name", "염정우", "part", "App");
        Map<String, Object> member2 = Map.of("name", "이가연", "part", "WEB-FE");
        Map<String, Object> member3 = Map.of("name", "권대훈", "part", "AI");

        return ApiResponse.success(List.of(member1, member2, member3));
    }

    // 3. 특정 멤버 상세 조회 (GET /api/v1/members/{uid})
    @GetMapping("/{uid}")
    public ApiResponse<Map<String, Object>> getMemberDetail(@PathVariable String uid) {
        Map<String, Object> mockMember = Map.of(
                "uid", uid,
                "name", "검색된 멤버",
                "bio", "이것은 " + uid + " 사용자의 상세 정보입니다."
        );

        return ApiResponse.success(mockMember);
    }
}