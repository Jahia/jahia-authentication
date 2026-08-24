package org.jahia.modules.jahiaauth.impl;

import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.JahiaAuthException;
import org.jahia.modules.jahiaauth.service.SsoIdentityLinkService;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

@Component(service = SsoIdentityLinkService.class, immediate = true)
public class SsoIdentityLinkServiceImpl implements SsoIdentityLinkService {

    private static final Logger logger = LoggerFactory.getLogger(SsoIdentityLinkServiceImpl.class);

    /**
     * Reads every link carrying one pair. The two values are string literals of a JCR-SQL2 statement,
     * so both are escaped for that context.
     */
    private static final String BY_PAIR = "SELECT * FROM [" + JahiaAuthConstants.SSO_IDENTITY_LINK_TYPE
            + "] WHERE [" + JahiaAuthConstants.PROPERTY_CONNECTOR_NAME + "] = '%s'"
            + " AND [" + JahiaAuthConstants.PROPERTY_SUBJECT + "] = '%s'";

    @Override
    public JCRUserNode resolveAccount(String connectorName, String subject, JCRSessionWrapper session)
            throws RepositoryException {
        if (isUnusable(connectorName) || isUnusable(subject)) {
            return null;
        }
        JCRUserNode resolved = null;
        NodeIterator links = query(connectorName, subject, session);
        while (links.hasNext()) {
            JCRUserNode account = accountOf((JCRNodeWrapper) links.nextNode());
            if (account == null) {
                continue;
            }
            if (resolved != null && !resolved.getPath().equals(account.getPath())) {
                // JCR states no unique constraint, so two accounts may come to carry one pair. Picking
                // one of them would sign in an account no rule names.
                logger.error("Resolved no account for connector {}: the asserted identity is carried by"
                        + " more than one account, {} and {}. An administrator has to remove the link"
                        + " that does not belong.", LogSafeValue.of(connectorName),
                        LogSafeValue.of(resolved.getPath()), LogSafeValue.of(account.getPath()));
                return null;
            }
            resolved = account;
        }
        return resolved;
    }

    @Override
    public void linkAccount(JCRUserNode account, String connectorName, String subject, JCRSessionWrapper session)
            throws JahiaAuthException, RepositoryException {
        if (isUnusable(connectorName)) {
            throw new JahiaAuthException("A link states which connector asserted the identity, and this"
                    + " one names none");
        }
        String refusal = SubjectCheck.refusalReason(subject);
        if (refusal != null) {
            throw new JahiaAuthException("The identity provider asserted a subject this framework cannot"
                    + " record: " + refusal);
        }
        JCRUserNode carrier = resolveAccount(connectorName, subject, session);
        if (carrier != null) {
            if (carrier.getPath().equals(account.getPath())) {
                return;
            }
            throw new JahiaAuthException("Another account already carries the identity connector "
                    + connectorName + " asserted, and one asserted identity belongs to one account");
        }
        JCRNodeWrapper container = linksNode(account);
        String name = linkNodeName(connectorName);
        if (container.hasNode(name)) {
            // One link per connector, which is what makes the node name derivable. A second identity of
            // one connector on one account is not a shape this framework writes.
            throw new JahiaAuthException("Account " + account.getName() + " already carries a link for"
                    + " connector " + connectorName);
        }
        JCRNodeWrapper link = container.addNode(name, JahiaAuthConstants.SSO_IDENTITY_LINK_TYPE);
        link.setProperty(JahiaAuthConstants.PROPERTY_CONNECTOR_NAME, connectorName);
        link.setProperty(JahiaAuthConstants.PROPERTY_SUBJECT, subject);
    }

    /**
     * Reads the node holding the links of an account, and creates it the first time.
     * <p>
     * Inheritance is broken and no role is granted, so the container is readable through a system
     * session alone: a system session does not pass through the access control list at all.
     * <p>
     * The owner of the account is granted nothing either, which is where this differs from a node a
     * person is meant to manage. A screen that shows a person their own links reads them through this
     * service rather than through the repository.
     */
    private static JCRNodeWrapper linksNode(JCRUserNode account) throws RepositoryException {
        if (account.hasNode(JahiaAuthConstants.SSO_IDENTITY_LINKS_NODE)) {
            return account.getNode(JahiaAuthConstants.SSO_IDENTITY_LINKS_NODE);
        }
        JCRNodeWrapper container = account.addNode(JahiaAuthConstants.SSO_IDENTITY_LINKS_NODE,
                JahiaAuthConstants.SSO_IDENTITY_LINKS_TYPE);
        container.setAclInheritanceBreak(true);
        return container;
    }

    /**
     * Reads the account a link belongs to.
     * <p>
     * A link sits in the container the account holds, so the account is two levels up. Both levels are
     * checked rather than assumed: a link written anywhere else in the repository answers the same
     * query, and the query is what decides which account a sign-in reaches.
     */
    private static JCRUserNode accountOf(JCRNodeWrapper link) throws RepositoryException {
        JCRNodeWrapper container = link.getParent();
        if (container.isNodeType(JahiaAuthConstants.SSO_IDENTITY_LINKS_TYPE)) {
            JCRNodeWrapper account = container.getParent();
            if (account instanceof JCRUserNode) {
                return (JCRUserNode) account;
            }
        }
        logger.warn("Ignored the link at {}: it is not held by an account.", LogSafeValue.of(link.getPath()));
        return null;
    }

    private static NodeIterator query(String connectorName, String subject, JCRSessionWrapper session)
            throws RepositoryException {
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        return queryManager.createQuery(statement(connectorName, subject), Query.JCR_SQL2)
                .execute().getNodes();
    }

    /**
     * The statement that reads the links carrying one pair.
     * <p>
     * Both values are string literals of a JCR-SQL2 statement, so both are escaped for that context. A
     * subject is a value an identity provider chose, and {@code SubjectCheck} accepts a quote inside
     * it on purpose: the escaping here is what makes the statement safe, and a check that refused the
     * quote would be the thing the statement rested on.
     * <p>
     * Built by a method of its own so that the escaping is measured without a repository.
     */
    static String statement(String connectorName, String subject) {
        return String.format(BY_PAIR, JCRContentUtils.sqlEncode(connectorName),
                JCRContentUtils.sqlEncode(subject));
    }

    /**
     * The name of the node carrying the link of one connector.
     * <p>
     * The connector names the node, so one account carries one link per connector. A node name takes
     * part in resolving an access control list and it is displayed, and the subject is neither of those
     * things.
     */
    private static String linkNodeName(String connectorName) {
        return "ssoIdentity-" + JCRContentUtils.generateNodeName(connectorName);
    }

    private static boolean isUnusable(String value) {
        return value == null || value.trim().isEmpty();
    }
}
