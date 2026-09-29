package com.noghterangi.catalog;

import java.util.regex.Pattern;

public final class PersianTextNormalizer {
    // Keep this set aligned with V6's PostgreSQL function, including half-space and NBSP.
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u0085\\u00a0\\u1680\\u2000-\\u200d\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff]+");
    private PersianTextNormalizer() {}
    public static String normalize(String value) {
        if (value == null) { return ""; }
        return WHITESPACE.matcher(value.replace('ي', 'ی').replace('ك', 'ک')).replaceAll(" ").trim();
    }
}
