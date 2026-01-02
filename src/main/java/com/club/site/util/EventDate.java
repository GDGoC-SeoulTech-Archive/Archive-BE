package com.club.site.util;

import com.club.site.web.ApiException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventDate {
    private static final Pattern ISO = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final Pattern KOREAN = Pattern.compile("^(\\d{4})??\\d{2})??\\d{2})??$");

    private EventDate() {
    }

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            throw new ApiException("BAD_REQUEST", "eventDate is required", HttpStatus.BAD_REQUEST);
        }
        String value = input.trim();
        Matcher iso = ISO.matcher(value);
        Matcher korean = KOREAN.matcher(value);
        if (iso.matches()) {
            ensureValid(iso.group(1), iso.group(2), iso.group(3), input);
            return value;
        }
        if (korean.matches()) {
            ensureValid(korean.group(1), korean.group(2), korean.group(3), input);
            return korean.group(1) + "-" + korean.group(2) + "-" + korean.group(3);
        }
        throw new ApiException("BAD_REQUEST", "eventDate must be YYYY-MM-DD or YYYY?„MM?”DD", HttpStatus.BAD_REQUEST);
    }

    private static void ensureValid(String yyyy, String mm, String dd, String original) {
        try {
            LocalDate.of(Integer.parseInt(yyyy), Integer.parseInt(mm), Integer.parseInt(dd));
        } catch (Exception e) {
            throw new ApiException("BAD_REQUEST", "Invalid eventDate: " + original, HttpStatus.BAD_REQUEST);
        }
    }
}


