package com.club.site.controller;

import com.club.site.dto.admin.AdminRoleRequest;
import com.club.site.dto.admin.AdminRoleResponse;
import com.club.site.security.FirebasePrincipal;
import com.club.site.service.AdminService;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/roles")
    public ApiResponse<AdminRoleResponse> setRole(Authentication authentication, @Valid @RequestBody AdminRoleRequest request) throws Exception {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        adminService.setRole(principal.uid(), request.uid(), request.role());
        return ApiResponse.ok(new AdminRoleResponse(request.uid(), request.role()));
    }
}


