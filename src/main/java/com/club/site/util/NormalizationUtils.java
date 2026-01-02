package com.club.site.util;

import java.text.Normalizer;
import java.util.Locale;

public class NormalizationUtils {
    private NormalizationUtils() {
    }

    public static String normalizeSkillId(String displayName) {
        if (displayName == null) {
            return null;
        }
        String s = Normalizer.normalize(displayName.trim(), Normalizer.Form.NFKC);
        s = s.toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("(^-+)|(-+$)", "");
        s = s.replaceAll("-{2,}", "-");
        return s;
    }
}


