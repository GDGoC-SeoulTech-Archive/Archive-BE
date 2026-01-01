package com.club.site.security;

import com.google.firebase.auth.FirebaseToken;

import java.util.Map;

public record FirebasePrincipal(
        String uid,
        String role,
        String displayName,
        String photoUrl,
        Map<String, Object> claims
) {
    public static FirebasePrincipal from(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        return new FirebasePrincipal(
                token.getUid(),
                String.valueOf(claims.getOrDefault("role", "user")),
                stringClaim(claims, "name"),
                stringClaim(claims, "picture"),
                claims
        );
    }

    private static String stringClaim(Map<String, Object> claims, String key) {
        Object v = claims.get(key);
        return v == null ? null : String.valueOf(v);
    }
}

