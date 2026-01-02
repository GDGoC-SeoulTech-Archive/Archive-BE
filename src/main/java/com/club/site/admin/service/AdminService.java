package com.club.site.admin.service;

import com.club.site.admin.SuperAdminAllowlist;
import com.club.site.common.service.AuditLogService;
import com.club.site.web.ApiException;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {
    private final SuperAdminAllowlist superAdminAllowlist;
    private final AuditLogService auditLogService;

    public AdminService(SuperAdminAllowlist superAdminAllowlist, AuditLogService auditLogService) {
        this.superAdminAllowlist = superAdminAllowlist;
        this.auditLogService = auditLogService;
    }

    public void setRole(String actorUid, String targetUid, String role) throws Exception {
        if (!superAdminAllowlist.isSuperAdmin(actorUid)) {
            throw new ApiException("FORBIDDEN", "Super admin only", HttpStatus.FORBIDDEN);
        }
        Map<String, Object> claims = new HashMap<>();
        if ("admin".equalsIgnoreCase(role)) {
            claims.put("role", "admin");
        }
        FirebaseAuth.getInstance().setCustomUserClaims(targetUid, claims);
        auditLogService.write("ROLE_SET", actorUid, targetUid, Map.of("role", role));
    }
}


