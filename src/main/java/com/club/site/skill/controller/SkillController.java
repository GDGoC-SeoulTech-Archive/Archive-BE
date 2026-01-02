package com.club.site.skill.controller;

import com.club.site.skill.dto.SkillCreateRequest;
import com.club.site.skill.dto.SkillDto;
import com.club.site.skill.dto.SkillPatchRequest;
import com.club.site.skill.dto.SkillsResponse;
import com.club.site.skill.service.SkillService;
import com.club.site.auth.dto.FirebasePrincipal;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {
    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ApiResponse<SkillsResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) throws Exception {
        return ApiResponse.ok(new SkillsResponse(skillService.list(activeOnly)));
    }

    @PostMapping
    public ApiResponse<SkillDto> create(Authentication authentication, @Valid @RequestBody SkillCreateRequest request) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        return ApiResponse.ok(skillService.create(request.displayName(), principal.uid()));
    }

    @PatchMapping("/{skillId}")
    public ApiResponse<SkillDto> patch(Authentication authentication, @PathVariable String skillId, @Valid @RequestBody SkillPatchRequest request) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        return ApiResponse.ok(skillService.patchActive(skillId, request.active(), principal.uid()));
    }
}


