package org.jahia.modules.jahiaauth.valves;

import org.jahia.modules.jahiaauth.impl.LogSafeValue;
import org.jahia.modules.jahiaauth.impl.session.SessionMapperResultsStore;
import org.jahia.modules.jahiaauth.service.MapperResult;
import org.jahia.modules.jahiaauth.service.SsoIdentityLinkService;
import org.jahia.services.content.JCRTemplate;
import org.jahia.params.valves.AuthValveContext;
import org.jahia.params.valves.BaseAuthValve;
import org.jahia.pipelines.Pipeline;
import org.jahia.pipelines.PipelineException;
import org.jahia.pipelines.valves.Valve;
import org.jahia.pipelines.valves.ValveContext;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.security.AuthenticationOptions;
import org.jahia.services.security.AuthenticationService;
import org.jahia.services.security.ConcurrentLoggedInUsersLimitExceededLoginException;
import org.jahia.services.security.InvalidSessionLoginException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;

@Component(service = Valve.class, immediate = true)
public class SSOValve extends BaseAuthValve {
    private static final Logger logger = LoggerFactory.getLogger(SSOValve.class);
    private static final String VALVE_RESULT = "login_valve_result";
    private static final String UNKNOWN_USER = "unknown_user";

    @Reference
    private SsoIdentityLinkService ssoIdentityLinkService;

    @Reference(target = "(type=authentication)")
    private Pipeline authPipeline;

    @Reference
    private AuthenticationService authenticationService;

    @Activate
    public void start() {
        setId("ssoValve");
        removeValve(authPipeline);
        addValve(authPipeline, -1, null, null);
    }

    @Deactivate
    public void stop() {
        removeValve(authPipeline);
    }

    @Override
    public void invoke(Object context, ValveContext valveContext) throws PipelineException {
        AuthValveContext authContext = (AuthValveContext) context;
        HttpServletRequest request = authContext.getRequest();

        if (authContext.getSessionFactory().getCurrentUser() != null) {
            valveContext.invokeNext(context);
            return;
        }

        // The site parameter gates whether the valve runs, and its value scopes nothing. The site an
        // account is resolved against comes from the result the connector recorded. The gate is read
        // first, so a request that is not a sign-in leaves the results of a sign-in in progress alone.
        if (!request.getParameterMap().containsKey("site")) {
            valveContext.invokeNext(context);
            return;
        }

        // This request is the sign-in the results were recorded for, so the session stops holding them
        // now. Every path below returns without reading the session again, and a refused sign-in
        // therefore leaves nothing behind.
        Map<String, MapperResult> results = SessionMapperResultsStore.take(request);
        if (results.isEmpty()) {
            valveContext.invokeNext(context);
            return;
        }

        SsoIdentity identity = findIdentity(results);
        if (identity == null) {
            valveContext.invokeNext(context);
            return;
        }

        boolean ok = false;
        ResolvedAccount account = resolveAccount(identity);

        if (account == null) {
            // An account of the login id's name may well exist. A sign-in reaches an account through
            // the link it carries for this connector, and an account carrying none is reached by an
            // administrator linking it.
            logger.warn("Login failed. Connector {} asserted an identity that no account of this"
                    + " installation carries. The account named {} is not reached by naming it.",
                    LogSafeValue.of(identity.getConnectorName()), LogSafeValue.of(identity.getUserId()));
            request.setAttribute(VALVE_RESULT, UNKNOWN_USER);
        } else {
            try {
                authenticationService.validateUserNode(account.getPath());
                ok = true;
            } catch (AccountLockedException e) {
                logger.warn("Login failed: account for user {} is locked.", account.getName());
                request.setAttribute(VALVE_RESULT, "account_locked");
            } catch (ConcurrentLoggedInUsersLimitExceededLoginException e) {
                logger.warn("Login failed. Maximum number of logged in users reached for {}", account.getName());
                request.setAttribute(VALVE_RESULT, "logged_in_users_limit_reached");
            }
        }

        if (ok) {
            login(authContext, request, results, account);
        } else {
            valveContext.invokeNext(context);
        }
    }

    /**
     * Reads the account that carries the identity the provider asserted.
     * <p>
     * The lookup runs in a system session, because the container an account holds its links in grants
     * nobody.
     * <p>
     * The account is read inside that session and described by its path and its name, which is all the
     * sign-in needs. Returning the node would hand a caller a node of a session that is already closed.
     *
     * @return the account, or {@code null} when no account carries the asserted identity, when more
     *         than one does, or when the account is one no connector resolves
     */
    private ResolvedAccount resolveAccount(SsoIdentity identity) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                JCRUserNode userNode = ssoIdentityLinkService.resolveAccount(identity.getConnectorName(),
                        identity.getSubject(), session);
                if (userNode == null) {
                    return null;
                }
                if (isNotResolvableThroughConnector(userNode)) {
                    logger.error("Login failed. User {} carries a link and is not resolvable through an"
                            + " authentication connector.", userNode.getName());
                    return null;
                }
                return new ResolvedAccount(userNode.getPath(), userNode.getName());
            });
        } catch (RepositoryException e) {
            logger.error("Login failed. Could not read which account carries the identity connector {}"
                    + " asserted.", LogSafeValue.of(identity.getConnectorName()), e);
            return null;
        }
    }

    private void login(AuthValveContext authContext, HttpServletRequest request, Map<String, MapperResult> results, ResolvedAccount account) {
        if (logger.isDebugEnabled()) {
            logger.debug("User {} logged in.", account.getPath());
        }

        boolean rememberMe = "on".equals(request.getParameter("useCookie"));
        AuthenticationOptions authenticationOptions = AuthenticationOptions.Builder.withDefaults()
                // the check is performed later in SessionAuthValveImpl
                .sessionValidityCheckEnabled(false)
                // pass the "remember me" flag
                .shouldRememberMe(rememberMe).build();
        // authenticate() destroys the session and fires the LOGIN event before it returns, so a
        // consumer reading the results in that event would find a session that does not hold them
        // yet. Publish them on the request first, which survives the swap.
        SessionMapperResultsStore.carryAcrossSignIn(request, results);
        try {
            authenticationService.authenticate(account.getPath(), authenticationOptions, authContext.getRequest(),
                    authContext.getResponse());

            request.setAttribute(VALVE_RESULT, "ok");
        } catch (InvalidSessionLoginException e) {
            // should not happen as the check is disabled
            throw new IllegalStateException("Unexpected InvalidSessionLoginException", e);
        } catch (AccountNotFoundException e) {
            // can only happen if the user was deleted after the lookup and before the authentication
            logger.warn("User not found : {}", account.getPath());
            request.setAttribute(VALVE_RESULT, UNKNOWN_USER);
        } finally {
            // Whether the sign-in ran or failed, nothing later in this request reads results that
            // answered it.
            SessionMapperResultsStore.stopCarrying(request);
        }
    }

    /**
     * The root account and {@code guest} sit outside the set of accounts an authentication connector
     * resolves. Nothing writes a link onto either one, so this refuses a link somebody else wrote.
     * Both definitions come from core, and the asymmetry is core's: {@code isRoot()} compares the node
     * identity, and {@code isGuest()} compares the name, which is only reachable on the implementation
     * class.
     */
    private static boolean isNotResolvableThroughConnector(JCRUserNode userNode) {
        return userNode.isRoot() || org.jahia.services.usermanager.JahiaUserManagerService.isGuest(userNode);
    }

    /**
     * Reads the identity the session authenticated as: the subject a provider asserted, the connector
     * that asserted it, the name of the account, and the site key.
     * <p>
     * A session holds one result per mapper, so several may name an account. Each result states a whole
     * identity or none, and the results that state one have to state the same one. A result stating a
     * part of an identity states none: the missing part would come from a second result and be picked
     * by the iteration order of an unordered map.
     * <p>
     * A result carrying no subject states no identity even when it names an account. The name is chosen
     * by the deployment and takes no part in resolving the account.
     *
     * @param results the results the session holds, keyed by mapper name
     * @return the identity to log in, or {@code null} when no result states one, when a result states
     *         part of one, or when the results state more than one
     */
    static SsoIdentity findIdentity(Map<String, MapperResult> results) {
        SsoIdentity identity = null;
        for (MapperResult result : results.values()) {
            if (result.getLoginId() == null && result.getSubject() == null) {
                continue;
            }
            if (result.getSubject() == null || result.getConnectorName() == null) {
                logger.warn("Login failed. Result naming account {} states no identity a provider"
                        + " asserted, so it describes nothing to resolve.",
                        LogSafeValue.of(result.getLoginId()));
                return null;
            }
            if (result.getLoginId() == null) {
                logger.warn("Login failed. Result for connector {} states an asserted identity and names"
                        + " no account, so it describes half an identity.",
                        LogSafeValue.of(result.getConnectorName()));
                return null;
            }
            if (result.getSiteKey() == null) {
                logger.warn("Login failed. Result for {} names an account and no site, so it describes"
                        + " no identity to resolve.", LogSafeValue.of(result.getLoginId()));
                return null;
            }
            SsoIdentity stated = new SsoIdentity(result.getLoginId(), result.getSiteKey(),
                    result.getConnectorName(), result.getSubject());
            if (identity != null && !identity.equals(stated)) {
                logger.warn("Login failed. The session authenticated two identities ({} and {}), so none"
                        + " is resolved.", LogSafeValue.of(identity.toString()), LogSafeValue.of(stated.toString()));
                return null;
            }
            identity = stated;
        }
        return identity;
    }

    static final class SsoIdentity {
        private final String userId;
        private final String siteKey;
        private final String connectorName;
        private final String subject;

        SsoIdentity(String userId, String siteKey, String connectorName, String subject) {
            this.userId = userId;
            this.siteKey = siteKey;
            this.connectorName = connectorName;
            this.subject = subject;
        }

        String getUserId() {
            return userId;
        }

        String getSiteKey() {
            return siteKey;
        }

        String getConnectorName() {
            return connectorName;
        }

        String getSubject() {
            return subject;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SsoIdentity)) {
                return false;
            }
            SsoIdentity other = (SsoIdentity) o;
            return Objects.equals(userId, other.userId) && Objects.equals(siteKey, other.siteKey)
                    && Objects.equals(connectorName, other.connectorName)
                    && Objects.equals(subject, other.subject);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, siteKey, connectorName, subject);
        }

        /**
         * Names the account and the connector, and never the subject. This value reaches a log line,
         * and the subject is what resolves an account.
         */
        @Override
        public String toString() {
            return userId + "@" + siteKey + " through " + connectorName;
        }
    }

    /**
     * An account this valve resolved, described by what the sign-in needs of it.
     * <p>
     * The lookup runs in a system session that is closed when it returns, so the node it read is not
     * carried out of it.
     */
    static final class ResolvedAccount {
        private final String path;
        private final String name;

        ResolvedAccount(String path, String name) {
            this.path = path;
            this.name = name;
        }

        String getPath() {
            return path;
        }

        String getName() {
            return name;
        }
    }
}
