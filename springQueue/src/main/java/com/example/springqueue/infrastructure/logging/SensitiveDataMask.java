package com.example.springqueue.infrastructure.logging;

public final class SensitiveDataMask {

    private static final String MASK = "*";

    private SensitiveDataMask() {}

    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "****";
        }
        if (value.length() <= 4) {
            return MASK.repeat(value.length());
        }
        return value.substring(0, 2)
                + MASK.repeat(value.length() - 4)
                + value.substring(value.length() - 2);
    }
}
