package org.jahia.modules.jahiaauth.valves;

import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.JahiaAuthMapperService;
import org.jahia.modules.jahiaauth.service.MappedProperty;
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
    private JahiaUserManagerService jahiaUserManagerService;

    @Reference
    private JahiaAuthMapperService jahiaAuthMapperService;

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

        String originalSessionId = request.getSession().getId();
        Map<String, Map<String, MappedProperty>> allMapperResult = jahiaAuthMapperService.getMapperResultsForSession(originalSessionId);
        // the site parameter gates whether the valve runs; its value scopes nothing — the site an
        // account is resolved against comes from the cached mapper result
        if (allMapperResult == null || !request.getParameterMap().containsKey("site")) {
            valveContext.invokeNext(context);
            return;
        }

        SsoIdentity identity = findIdentity(allMapperResult);
        if (identity == null) {
            valveContext.invokeNext(context);
            return;
        }

        if (identity.getSiteKey() == null) {
            // the site an account is resolved against is the one the connector is configured on, and
            // executeMapper records it on every entry it caches. An entry without it comes from
            // somewhere else — a connector seeding the cache directly, or a previous version of the
            // bundle whose entries are still live — and resolving it at server level instead would
            // answer differently for that same entry once a current connector has re-cached it.
            logger.warn("Login failed. The cached identity for {} carries no site key.", identity.getUserId());
            request.setAttribute(VALVE_RESULT, UNKNOWN_USER);
            valveContext.invokeNext(context);
            return;
        }

        boolean ok = false;
        JCRUserNode userNode = jahiaUserManagerService.lookupUser(identity.getUserId(), identity.getSiteKey());

        if (userNode == null) {
            logger.warn("Login failed. Unknown username {} on site {}", identity.getUserId(), identity.getSiteKey());
            request.setAttribute(VALVE_RESULT, UNKNOWN_USER);
        } else if (isNotResolvableThroughConnector(userNode)) {
            logger.warn("Login failed. User {} is not resolvable through an authentication connector.", userNode.getName());
            request.setAttribute(VALVE_RESULT, UNKNOWN_USER);
        } else {
            try {
                authenticationService.validateUserNode(userNode.getPath());
                ok = true;
            } catch (AccountLockedException e) {
                logger.warn("Login failed: account for user {} is locked.", userNode.getName());
                request.setAttribute(VALVE_RESULT, "account_locked");
            } catch (ConcurrentLoggedInUsersLimitExceededLoginException e) {
                logger.warn("Login failed. Maximum number of logged in users reached for {}", userNode.getName());
                request.setAttribute(VALVE_RESULT, "logged_in_users_limit_reached");
            }
        }

        if (ok) {
            login(authContext, request, originalSessionId, userNode);
        } else {
            valveContext.invokeNext(context);
        }
    }

    private void login(AuthValveContext authContext, HttpServletRequest request, String originalSessionId, JCRUserNode userNode) {
        if (logger.isDebugEnabled()) {
            logger.debug("User {} logged in.", userNode);
        }

        boolean rememberMe = "on".equals(request.getParameter("useCookie"));
        AuthenticationOptions authenticationOptions = AuthenticationOptions.Builder.withDefaults()
                // the check is performed later in SessionAuthValveImpl
                .sessionValidityCheckEnabled(false)
                // pass the "remember me" flag
                .shouldRememberMe(rememberMe).build();
        try {
            authenticationService.authenticate(userNode.getPath(), authenticationOptions, authContext.getRequest(),
                    authContext.getResponse());

            // update the cache entry if a new session was created
            if (!originalSessionId.equals(request.getSession().getId())) {
                jahiaAuthMapperService.updateCacheEntry(originalSessionId, request.getSession().getId());
            }
            request.setAttribute(VALVE_RESULT, "ok");
        } catch (InvalidSessionLoginException e) {
            // should not happen as the check is disabled
            throw new IllegalStateException("Unexpected InvalidSessionLoginException", e);
        } catch (AccountNotFoundException e) {
            // can only happen if the user was deleted after the lookup and before the authentication
            logger.warn("User not found : {}", userNode.getPath());
            request.setAttribute(VALVE_RESULT, UNKNOWN_USER);
        }
    }

    /**
     * The root account and {@code guest} sit outside the set of accounts an authentication connector
     * resolves. Both definitions come from core, and the asymmetry is core's: {@code isRoot()} compares
     * the node identity, {@code isGuest()} the name — which is why the latter is reached on the
     * implementation class rather than on the {@link JahiaUserManagerService} API this valve holds.
     */
    private static boolean isNotResolvableThroughConnector(JCRUserNode userNode) {
        return userNode.isRoot() || org.jahia.services.usermanager.JahiaUserManagerService.isGuest(userNode);
    }

    /**
     * Reads the identity the session authenticated as: a login id and the site key it is resolved
     * against, taken from one and the same mapper result so both describe the same connector.
     * <p>
     * A session holds one cached result per mapper, so several may carry a login id — one per
     * connector the session authenticated through. They have to agree: two that name different
     * accounts, or the same account on different sites, leave no defined identity to sign in, and
     * picking one of them would come down to the iteration order of an unordered map. No account is
     * resolved in that case.
     *
     * @param allMapperResult the mapper results cached for the session
     * @return the identity to log in, {@code null} when no mapper result holds a login id or when the
     *         results hold more than one identity
     */
    static SsoIdentity findIdentity(Map<String, Map<String, MappedProperty>> allMapperResult) {
        SsoIdentity identity = null;
        for (Map<String, MappedProperty> mapperResult : allMapperResult.values()) {
            SsoIdentity candidate = identityOf(mapperResult);
            if (candidate == null) {
                continue;
            }
            if (identity != null && !identity.equals(candidate)) {
                logger.warn("Login failed. The session authenticated as several identities ({} and {}), so none is resolved.",
                        identity, candidate);
                return null;
            }
            identity = candidate;
        }
        return identity;
    }

    private static SsoIdentity identityOf(Map<String, MappedProperty> mapperResult) {
        MappedProperty login = mapperResult.get(JahiaAuthConstants.SSO_LOGIN);
        if (login == null || login.getValue() == null) {
            return null;
        }
        MappedProperty site = mapperResult.get(JahiaAuthConstants.SITE_KEY);
        return new SsoIdentity((String) login.getValue(), site != null ? (String) site.getValue() : null);
    }

    static final class SsoIdentity {
        private final String userId;
        private final String siteKey;

        SsoIdentity(String userId, String siteKey) {
            this.userId = userId;
            this.siteKey = siteKey;
        }

        String getUserId() {
            return userId;
        }

        String getSiteKey() {
            return siteKey;
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
            return Objects.equals(userId, other.userId) && Objects.equals(siteKey, other.siteKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, siteKey);
        }

        @Override
        public String toString() {
            return userId + "@" + siteKey;
        }
    }
}
