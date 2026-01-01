package com.club.site.dto;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        String nextCursor
) {
}


