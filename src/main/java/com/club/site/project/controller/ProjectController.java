package com.club.site.project.controller;

import com.club.site.auth.dto.FirebasePrincipal;
import com.club.site.common.error.ErrorCode;
import com.club.site.common.exception.BusinessException;
import com.club.site.project.dto.ProjectCreateRequest;
import com.club.site.project.dto.ProjectCreateResponse;
import com.club.site.project.dto.ProjectDeleteResponse;
import com.club.site.project.dto.ProjectDetailResponse;
import com.club.site.project.dto.ProjectDto;
import com.club.site.project.dto.ProjectListResponse;
import com.club.site.project.dto.ProjectUpdateRequest;
import com.club.site.project.dto.ProjectUpdateResponse;
import com.club.site.project.service.ProjectService;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 프로젝트 목록 조회 (Public)
     * GET /api/v1/projects?pageSize=20&cursor=...
     */
    @GetMapping
    public ApiResponse<ProjectListResponse> list(
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) throws Exception {
        ProjectListResponse response = projectService.listProjects(pageSize, cursor);
        return ApiResponse.ok(response);
    }

    /**
     * 프로젝트 상세 조회 (Public)
     * GET /api/v1/projects/{projectId}
     */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDetailResponse> get(@PathVariable String projectId) throws Exception {
        ProjectDto projectDto = projectService.getProject(projectId);
        ProjectDetailResponse.ProjectDetail detail = ProjectDetailResponse.ProjectDetail.from(projectDto);
        return ApiResponse.ok(new ProjectDetailResponse(detail));
    }

    /**
     * 프로젝트 생성 (Admin)
     * POST /api/v1/projects
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectCreateResponse>> create(
            Authentication authentication,
            @Valid @RequestBody ProjectCreateRequest request
    ) throws Exception {
        requireAdmin(authentication);
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();

        ProjectDto projectDto = projectService.createProject(principal.uid(), request);
        ProjectDetailResponse.ProjectDetail detail = ProjectDetailResponse.ProjectDetail.from(projectDto);
        ProjectCreateResponse response = new ProjectCreateResponse(projectDto.id(), detail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * 프로젝트 수정 (Admin)
     * PUT /api/v1/projects/{projectId}
     */
    @PutMapping("/{projectId}")
    public ApiResponse<ProjectUpdateResponse> update(
            Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody ProjectUpdateRequest request
    ) throws Exception {
        requireAdmin(authentication);
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();

        ProjectDto projectDto = projectService.updateProject(principal.uid(), projectId, request);
        ProjectDetailResponse.ProjectDetail detail = ProjectDetailResponse.ProjectDetail.from(projectDto);
        ProjectUpdateResponse response = new ProjectUpdateResponse(projectId, detail);

        return ApiResponse.ok(response);
    }

    /**
     * 프로젝트 삭제 (Admin)
     * DELETE /api/v1/projects/{projectId}
     */
    @DeleteMapping("/{projectId}")
    public ApiResponse<ProjectDeleteResponse> delete(
            Authentication authentication,
            @PathVariable String projectId
    ) throws Exception {
        requireAdmin(authentication);
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();

        projectService.deleteProject(principal.uid(), projectId);
        return ApiResponse.ok(new ProjectDeleteResponse(true));
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException(ErrorCode.ADMIN_REQUIRED);
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            throw new BusinessException(ErrorCode.ADMIN_REQUIRED);
        }
    }
}
