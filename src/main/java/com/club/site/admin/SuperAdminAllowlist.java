package com.club.site.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SuperAdminAllowlist {
    private final Set<String> uids;

    public SuperAdminAllowlist(@Value("${SUPER_ADMIN_UIDS:}") String superAdminUids) {
        this.uids = Arrays.stream(superAdminUids.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isSuperAdmin(String uid) {
        return uid != null && uids.contains(uid);
    }
}


