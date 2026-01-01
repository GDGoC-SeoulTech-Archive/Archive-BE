package com.club.site.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminRoleRequest(
        @NotBlank String uid,
        @NotBlank @Pattern(regexp = "^(admin|user)$", message = "role must be admin|user") String role
) {
}


