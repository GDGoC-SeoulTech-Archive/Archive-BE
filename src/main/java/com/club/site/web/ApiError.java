package com.club.site.web;

public record ApiError(
        String code,
        String message
) {
}

