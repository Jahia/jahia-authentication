package org.jahia.modules.jahiaauth.service;

import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;

import javax.jcr.RepositoryException;

/**
 * Resolves and records which account carries the identity an identity provider asserted.
 * <p>
 * An account is resolved by the pair of the connector and the subject, and never by its name. The
 * name of an account is chosen by the deployment, and it takes no part in the resolution. The subject
 * is stored beside the connector that asserted it, because two providers may assert the same subject
 * string.
 * <p>
 * An account holds zero or more links. This framework writes the first one, when it creates the
 * account. A second link on one account is a shape the model accepts, and nothing here produces it:
 * writing one correlates two providers, and the only value that correlates them is the mail address.
 * An already authenticated session is the context in which a second link belongs, and adding one is
 * not part of this interface.
 * <p>
 * JCR states no unique constraint, so uniqueness is stated here. A pair another account already
 * carries is refused, and a pair more than one account answers resolves nobody.
 */
public interface SsoIdentityLinkService {

    /**
     * Reads the account that carries one asserted identity.
     *
     * @param connectorName the connector whose identity provider asserted the subject
     * @param subject the subject the identity provider asserted
     * @param session a system session, which is the only session a link is readable through
     * @return the account carrying the pair, or {@code null} when no account carries it and when more
     *         than one does
     * @throws RepositoryException if the repository cannot be read
     */
    JCRUserNode resolveAccount(String connectorName, String subject, JCRSessionWrapper session)
            throws RepositoryException;

    /**
     * Records that an account carries one asserted identity.
     * <p>
     * The caller saves the session. Nothing is written when the pair is refused.
     *
     * @param account the account to link
     * @param connectorName the connector whose identity provider asserted the subject
     * @param subject the subject the identity provider asserted
     * @param session the session the account was read or created in
     * @throws JahiaAuthException when another account already carries the pair, when this account
     *         already carries a link for the connector, or when either value is unusable
     * @throws RepositoryException if the repository cannot be written
     */
    void linkAccount(JCRUserNode account, String connectorName, String subject, JCRSessionWrapper session)
            throws JahiaAuthException, RepositoryException;
}
