package com.club.site.util;

import com.club.site.web.ApiException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class UrlUtils {
    private UrlUtils() {
    }

    public static void requireHttpUrl(String url, String fieldName) {
        if (url == null || url.isBlank()) {
            throw new ApiException("BAD_REQUEST", fieldName + " is required", HttpStatus.BAD_REQUEST);
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new ApiException("BAD_REQUEST", "Invalid URL for " + fieldName, HttpStatus.BAD_REQUEST);
        }
    }
}


