package com.club.site.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.club.site.web.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

public enum Part {
    WEB_FE("WEB·FE"),
    WEB_BE("WEB·BE"),
    AI("AI"),
    APP("App"),
    DESIGN("Design");

    private final String wireValue;

    Part(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static Part parse(String value) {
        if (value == null) {
            throw new ApiException("BAD_REQUEST", "part is required", HttpStatus.BAD_REQUEST);
        }
        String normalized = value.trim()
                .replace("·", "_")
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase(Locale.ROOT);
        normalized = normalized.replace("WEB__FE", "WEB_FE").replace("WEB__BE", "WEB_BE");
        try {
            return Part.valueOf(normalized);
        } catch (Exception e) {
            throw new ApiException("BAD_REQUEST", "Invalid part: " + value, HttpStatus.BAD_REQUEST);
        }
    }
}

