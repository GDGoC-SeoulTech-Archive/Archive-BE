package com.club.site.util;

public class HtmlUtils {
    private HtmlUtils() {
    }

    /**
     * HTML 태그를 이스케이프하여 Plain Text로 변환
     * < -> &lt;
     * > -> &gt;
     * & -> &amp;
     * " -> &quot;
     * ' -> &#39;
     * 
     * @param text 원본 텍스트
     * @return HTML 이스케이프된 텍스트
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

