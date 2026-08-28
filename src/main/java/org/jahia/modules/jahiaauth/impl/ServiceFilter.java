package org.jahia.modules.jahiaauth.impl;

/**
 * Builds the filter that selects one OSGi service by name.
 * <p>
 * The name often comes from a request or from a configuration file, and a filter is a small language:
 * an unescaped {@code *} matches every service rather than one, and an unbalanced parenthesis raises
 * out of the caller instead of answering it. This class escapes the value, so the filter selects the
 * service that carries that name and nothing else.
 */
public final class ServiceFilter {

    /** The character a filter reads as the end of a string, escaped as RFC 1960 writes it. */
    private static final char NUL = (char) 0;

    private ServiceFilter() {
        // Utility class
    }

    /**
     * @param attribute the service property to match
     * @param value the name to match it against, escaped here
     * @return the filter, which selects at most one service
     */
    public static String byName(String attribute, String value) {
        return "(" + attribute + "=" + escape(value) + ")";
    }

    /**
     * Escapes the five characters a filter reads as syntax, as RFC 1960 states them.
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                escaped.append("\\5c");
            } else if (c == '*') {
                escaped.append("\\2a");
            } else if (c == '(') {
                escaped.append("\\28");
            } else if (c == ')') {
                escaped.append("\\29");
            } else if (c == NUL) {
                escaped.append("\\00");
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
