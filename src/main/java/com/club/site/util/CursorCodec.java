package com.club.site.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CursorCodec {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public record Cursor(long tsMillis, String id) {
    }

    public static String encode(Cursor cursor) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    public static Cursor decodeOrNull(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(encoded);
            return objectMapper.readValue(new String(json, StandardCharsets.UTF_8), Cursor.class);
        } catch (Exception e) {
            return null;
        }
    }
}


