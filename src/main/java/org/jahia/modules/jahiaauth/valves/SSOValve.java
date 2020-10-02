package org.jahia.modules.jahiaauth.valves;

import org.jahia.api.Constants;
import org.jahia.api.settings.SettingsBean;
import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.JahiaAuthMapperService;
import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.osgi.FrameworkService;
import org.jahia.params.valves.AuthValveContext;
import org.jahia.params.valves.BaseAuthValve;
import org.jahia.params.valves.CookieAuthValveImpl;
import org.jahia.pipelines.Pipeline;
import org.jahia.pipelines.PipelineException;
import org.jahia.pipelines.valves.ValveContext;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.preferences.user.UserPreferencesHelper;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.utils.LanguageCodeConverters;
import org.jahia.utils.Patterns;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SSOValve extends BaseAuthValve {
    private static final Logger logger = LoggerFactory.getLogger(SSOValve.class);
    private static String VALVE_RESULT = "login_valve_result";

    private JahiaUserManagerService jahiaUserManagerService;
    private JahiaAuthMapperService jahiaAuthMapperService;
    private SettingsBean settingsBean;
    private Pipeline authPipeline;
    private String preserveSessionAttributes = null;

    public void start() {
        setId("ssoValve");
        removeValve(authPipeline);
        addValve(authPipeline, -1, null, null);
    }

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
            if (!userNode.isAccountLocked()) {
                ok = true;
            } else {
                logger.warn("Login failed: account for user {} is locked.", userNode.getName());
                request.setAttribute(VALVE_RESULT, "account_locked");
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

        // if there are any attributes to conserve between session, let's copy them into a map first
        Map<String, Serializable> savedSessionAttributes = preserveSessionAttributes(request);

        JahiaUser jahiaUser = userNode.getJahiaUser();

        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }

        if (!originalSessionId.equals(request.getSession().getId())) {
            jahiaAuthMapperService.updateCacheEntry(originalSessionId, request.getSession().getId());
        }

        // if there were saved session attributes, we restore them here.
        restoreSessionAttributes(request, savedSessionAttributes);

        request.setAttribute(VALVE_RESULT, "ok");
        authContext.getSessionFactory().setCurrentUser(jahiaUser);

        // do a switch to the user's preferred language
        if (settingsBean.isConsiderPreferredLanguageAfterLogin()) {
            Locale preferredUserLocale = UserPreferencesHelper.getPreferredLocale(userNode, LanguageCodeConverters.resolveLocaleForGuest(request));
            request.getSession().setAttribute(Constants.SESSION_LOCALE, preferredUserLocale);
        }

        String useCookie = request.getParameter("useCookie");
        if ((useCookie != null) && ("on".equals(useCookie))) {
            // the user has indicated he wants to use cookie authentication
            CookieAuthValveImpl.createAndSendCookie(authContext, userNode, settingsBean.getCookieAuthConfig());
        }

        Map<String, Object> m = new HashMap<>();
        m.put("user", jahiaUser);
        m.put("authContext", authContext);
        m.put("source", this);
        FrameworkService.sendEvent("org/jahia/usersgroups/login/LOGIN", m, false);
    }

    @Nullable
    private String findUserId(Map<String, Map<String, MappedProperty>> allMapperResult) {
        for (Map<String, MappedProperty> mapperResult : allMapperResult.values()) {
            if (mapperResult.containsKey(JahiaAuthConstants.SSO_LOGIN)) {
                return (String) mapperResult.get(JahiaAuthConstants.SSO_LOGIN).getValue();
            }
        }
        return null;
    }

    private Map<String, Serializable> preserveSessionAttributes(HttpServletRequest httpServletRequest) {
        Map<String, Serializable> savedSessionAttributes = new HashMap<>();
        if ((preserveSessionAttributes != null) && (httpServletRequest.getSession(false) != null) && (preserveSessionAttributes.length() > 0)) {
            String[] sessionAttributeNames = Patterns.TRIPLE_HASH.split(preserveSessionAttributes);
            HttpSession session = httpServletRequest.getSession(false);
            for (String sessionAttributeName : sessionAttributeNames) {
                Object attributeValue = session.getAttribute(sessionAttributeName);
                if (attributeValue instanceof Serializable) {
                    savedSessionAttributes.put(sessionAttributeName, (Serializable) attributeValue);
                }
            }
        }
        return savedSessionAttributes;
    }

    private void restoreSessionAttributes(HttpServletRequest httpServletRequest, Map<String, Serializable> savedSessionAttributes) {
        if (savedSessionAttributes.size() > 0) {
            HttpSession session = httpServletRequest.getSession();
            for (Map.Entry<String, Serializable> savedSessionAttribute : savedSessionAttributes.entrySet()) {
                session.setAttribute(savedSessionAttribute.getKey(), savedSessionAttribute.getValue());
            }
        }
    }

    public void setJahiaAuthMapperService(JahiaAuthMapperService jahiaAuthMapperService) {
        this.jahiaAuthMapperService = jahiaAuthMapperService;
    }

    public void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public void setSettingsBean(SettingsBean settingsBean) {
        this.settingsBean = settingsBean;
        this.preserveSessionAttributes = settingsBean.getString("preserveSessionAttributesOnLogin", "wemSessionId");
    }

    public void setAuthPipeline(Pipeline authPipeline) {
        this.authPipeline = authPipeline;
    }
}
