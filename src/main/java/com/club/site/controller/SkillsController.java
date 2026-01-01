package com.club.site.controller;

import com.club.site.dto.skill.SkillCreateRequest;
import com.club.site.dto.skill.SkillDto;
import com.club.site.dto.skill.SkillPatchRequest;
import com.club.site.dto.skill.SkillsResponse;
import com.club.site.security.FirebasePrincipal;
import com.club.site.service.SkillService;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillsController {
    private final SkillService skillService;

    public SkillsController(SkillService skillService) {
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


