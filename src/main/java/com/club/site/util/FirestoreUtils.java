package com.club.site.util;

import com.google.cloud.Timestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FirestoreUtils {
    private FirestoreUtils() {
    }

    public static String toIsoString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return instant.toString();
    }

    public static Timestamp fromMillis(long tsMillis) {
        if (tsMillis <= 0) {
            return null;
        }
        long seconds = tsMillis / 1000L;
        int nanos = (int) ((tsMillis % 1000L) * 1_000_000L);
        return Timestamp.ofTimeSecondsAndNanos(seconds, nanos);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                out.add((Map<String, Object>) map);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                out.add(String.valueOf(item));
            }
        }
        return out;
    }
}


