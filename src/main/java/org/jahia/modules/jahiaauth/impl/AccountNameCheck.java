package org.jahia.modules.jahiaauth.impl;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Decides whether a value an identity provider returned may become the name of a Jahia account.
 * <p>
 * The name reaches the repository as a path, so it is held to what a name may contain rather than
 * passed on as it arrives. A value outside that set is refused and never rewritten: rewriting would
 * let two values the provider keeps apart resolve to one account.
 */
public final class AccountNameCheck {

    /**
     * Letters and digits of any script, and the four separators an account name carries in practice.
     * Subject identifiers, UUIDs, logins and mail addresses are inside it. Everything the repository
     * reads as syntax is outside it: the path separator, the brackets of an identifier segment, the
     * colon of a namespace prefix, the wildcards of a query, quotes and whitespace.
     */
    private static final Pattern ACCOUNT_NAME = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}._@+-]{0,254}");

    private AccountNameCheck() {
        // Utility class
    }

    /**
     * @param value the value a mapper read for the login id, as the connector returned it
     * @return the reason this value may not name an account, or {@code null} when it may
     */
    public static String refusalReason(Object value) {
        if (value == null) {
            return "the connector returned no value";
        }
        if (value instanceof Collection || value.getClass().isArray()) {
            // A claim may legitimately carry several values, and the two may name different accounts.
            // One of them is chosen by no rule this framework states, and joining them names an account
            // neither value states.
            return "the connector returned several values, and an account is one name";
        }
        String name = value.toString();
        if (!ACCOUNT_NAME.matcher(name).matches()) {
            return "the value is not the name of an account";
        }
        return null;
    }
}
