package org.jahia.modules.jahiaauth.impl;

import java.util.Collection;

/**
 * Decides whether a value an identity provider asserted may be recorded as the subject of a link.
 * <p>
 * The subject names no node and no path. It is a value the framework stores and compares, so it is
 * held to far less than an account name is: a subject carrying a quote, a slash or a space is
 * recorded as it arrives. The statement that reads it back escapes it for the context it is printed
 * in, and this check is not what makes that statement safe. Widening this set therefore breaks
 * nothing, which is the property {@link AccountNameCheck} deliberately does not have.
 * <p>
 * What is refused is a value that describes no single identity, and a value too long to belong in the
 * repository.
 */
public final class SubjectCheck {

    /**
     * A subject is an identifier, and no identity provider states one longer than this. The bound
     * exists so that a response this framework did not produce cannot write an unbounded value.
     */
    private static final int MAX_LENGTH = 512;

    private SubjectCheck() {
        // Utility class
    }

    /**
     * @param value the value the connector returned for the property it declares as its verified
     *        subject
     * @return the reason this value may not be recorded as a subject, or {@code null} when it may
     */
    public static String refusalReason(Object value) {
        if (value == null) {
            return "the connector returned no value";
        }
        if (value instanceof Collection || value.getClass().isArray()) {
            // Two values describe two identities, and joining them describes a third that the provider
            // never asserted.
            return "the connector returned several values, and one asserted identity is one value";
        }
        String subject = value.toString();
        if (subject.trim().isEmpty()) {
            return "the connector returned a blank value";
        }
        if (subject.length() > MAX_LENGTH) {
            return "the value is longer than " + MAX_LENGTH + " characters";
        }
        return null;
    }
}
