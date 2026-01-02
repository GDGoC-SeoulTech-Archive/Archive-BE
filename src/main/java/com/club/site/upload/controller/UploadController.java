package com.club.site.upload.controller;

import com.club.site.upload.dto.UploadPresignRequest;
import com.club.site.upload.dto.UploadPresignResponse;
import com.club.site.upload.service.UploadService;
import com.club.site.auth.dto.FirebasePrincipal;
import com.club.site.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {
    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/presign")
    public ApiResponse<UploadPresignResponse> presign(Authentication authentication, @Valid @RequestBody UploadPresignRequest request) {
        FirebasePrincipal principal = (FirebasePrincipal) authentication.getPrincipal();
        UploadService.PresignResult result = uploadService.presign(principal.uid(), request.contentType(), request.fileName());
        return ApiResponse.ok(new UploadPresignResponse(result.uploadUrl(), result.publicUrl()));
    }
}


