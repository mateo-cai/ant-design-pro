package com.gdstc.beszel.util;

/** Java 8-friendly string helpers (replaces String.isBlank). */
public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}
