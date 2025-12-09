package org.jahia.modules.jahiaauth.valves;

import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.JahiaAuthMapperService;
import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.params.valves.AuthValveContext;
import org.jahia.params.valves.BaseAuthValve;
import org.jahia.pipelines.Pipeline;
import org.jahia.pipelines.PipelineException;
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

@Component(immediate = true)
public class SSOValve extends BaseAuthValve {
    private static final Logger logger = LoggerFactory.getLogger(SSOValve.class);
    private static final String VALVE_RESULT = "login_valve_result";

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
        if (allMapperResult == null || !request.getParameterMap().containsKey("site")) {
            valveContext.invokeNext(context);
            return;
        }

        String userId = findUserId(allMapperResult);
        if (userId == null) {
            valveContext.invokeNext(context);
            return;
        }

        boolean ok = false;
        String siteKey = request.getParameter("site");
        JCRUserNode userNode = jahiaUserManagerService.lookupUser(userId, siteKey);

        if (userNode != null) {
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
        } else {
            logger.warn("Login failed. Unknown username {}", userId);
            request.setAttribute(VALVE_RESULT, "unknown_user");
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
            request.setAttribute(VALVE_RESULT, "unknown_user");
        }
    }

    private String findUserId(Map<String, Map<String, MappedProperty>> allMapperResult) {
        for (Map<String, MappedProperty> mapperResult : allMapperResult.values()) {
            if (mapperResult.containsKey(JahiaAuthConstants.SSO_LOGIN)) {
                return (String) mapperResult.get(JahiaAuthConstants.SSO_LOGIN).getValue();
            }
        }
        return null;
    }
}
