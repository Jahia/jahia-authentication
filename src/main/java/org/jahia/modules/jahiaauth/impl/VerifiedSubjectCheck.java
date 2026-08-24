package org.jahia.modules.jahiaauth.impl;

import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;

/**
 * Refuses a connector that states no identity a sign-in can resolve an account by.
 * <p>
 * The account a sign-in reaches is resolved by the subject the identity provider asserted, paired
 * with the connector that asserted it. The connector names the property carrying that subject, and a
 * configuration cannot name another one: the framework reads the declared property and reads no
 * mapping to find it. A connector that declares none therefore lets no sign-in resolve an account,
 * and this check says so.
 * <p>
 * The rule is stated over the connector and not over a mapping. A mapping feeds the name of the
 * account and the profile properties, and the name takes no part in resolving the account, so a
 * mapping states nothing this check would read.
 */
public final class VerifiedSubjectCheck {

    private VerifiedSubjectCheck() {
        // Utility class
    }

    /**
     * @param connectorName the connector a sign-in runs through
     * @param verifiedSubjectProperty the property that connector declares as carrying the subject its
     *        identity provider verified, or {@code null} when it declares none
     * @return the reason no sign-in through this connector resolves an account, or {@code null} when
     *         the connector states one
     */
    public static String refusalReason(String connectorName, String verifiedSubjectProperty) {
        if (verifiedSubjectProperty != null && !verifiedSubjectProperty.trim().isEmpty()) {
            return null;
        }
        return String.format(
                "Connector %s names no verified subject property, so nothing states which of its"
                + " properties carries an identity its provider stands behind. An account is resolved by"
                + " that identity and not by the name held by %s, so no sign-in through this connector"
                + " resolves an account. Have the connector return that property name from"
                + " getVerifiedSubjectProperty.",
                LogSafeValue.of(connectorName), JahiaAuthConstants.SSO_LOGIN);
    }
}
