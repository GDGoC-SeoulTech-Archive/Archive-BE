package com.club.site.common.dto;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        String nextCursor
) {
}

