package org.jahia.modules.jahiaauth.impl;

/**
 * Makes a value from an identity provider safe to write to a log.
 * <p>
 * A claim is a value the identity provider supplies, so its content is decided outside this
 * application. A log line carries one entry, and a value holding a line break would spread one entry
 * over several lines, so a line break is replaced. A log line is of a bounded length as well, so a
 * long value is truncated.
 * <p>
 * Call this on every value that came from a claim, and on a configured value too, since the cost is
 * one pass over a short string.
 */
public final class LogSafeValue {

    static final int MAX_LENGTH = 120;
    private static final String TRUNCATED = "...(truncated)";
    private static final String REPLACEMENT = "?";

    private LogSafeValue() {
        // Utility class
    }

    /**
     * @param value the value to write, may be {@code null}
     * @return the value with every control character replaced and its length capped
     */
    public static String of(String value) {
        if (value == null) {
            return "null";
        }
        String capped = value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) + TRUNCATED : value;
        StringBuilder safe = new StringBuilder(capped.length());
        for (int i = 0; i < capped.length(); i++) {
            char c = capped.charAt(i);
            // A control character is what breaks a line or moves a cursor, and a log line holds none.
            safe.append(Character.isISOControl(c) ? REPLACEMENT : c);
        }
        return safe.toString();
    }
}
