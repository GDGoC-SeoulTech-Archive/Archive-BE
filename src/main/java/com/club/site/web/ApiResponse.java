package com.club.site.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        Object meta,
        ApiError error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, Map.of(), null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(null, Map.of(), null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(null, null, new ApiError(code, message));
    }
}

