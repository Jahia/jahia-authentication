package org.jahia.modules.jahiaauth.impl.session;

import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.modules.jahiaauth.service.MappedPropertyInfo;
import org.jahia.modules.jahiaauth.service.MapperResult;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Every path that records an identity passes the same check.
 * <p>
 * A connector records what its provider returned, and a mapper decides which account that describes.
 * These oracles drive the connector path, which is the one that reaches the session without passing
 * through the mappings.
 */
public class ConnectorRecordingTest {

    private static MappedProperty property(String name, String value) {
        return new MappedProperty(new MappedPropertyInfo(name), value);
    }

    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static Map<String, MapperResult> record(Map<String, MappedProperty> properties) {
        Map<String, Object> sessionAttributes = new HashMap<>();
        HttpSession session = (HttpSession) proxy(HttpSession.class, (p, method, args) -> {
            switch (method.getName()) {
                case "getAttribute": return sessionAttributes.get((String) args[0]);
                case "setAttribute": return sessionAttributes.put((String) args[0], args[1]);
                case "removeAttribute": return sessionAttributes.remove((String) args[0]);
                case "getAttributeNames": return Collections.enumeration(new ArrayList<>(sessionAttributes.keySet()));
                default: return null;
            }
        });
        HttpServletRequest request = (HttpServletRequest) proxy(HttpServletRequest.class, (p, method, args) -> {
            switch (method.getName()) {
                case "getSession": return session;
                default: return null;
            }
        });
        new JahiaAuthMapperServiceImpl().recordConnectorProperties(request, "KeycloakApi", properties);
        return SessionMapperResultsStore.getAll(request);
    }

    @Test
    public void shouldRecordWhatTheConnectorReturned() {
        Map<String, MapperResult> results = record(
                Collections.singletonMap("j:email", property("j:email", "alice@example.com")));

        assertEquals("alice@example.com", results.get("KeycloakApi").getProperties().get("j:email").getValue());
    }

    @Test
    public void shouldNameNoAccountFromTheConnectorPath() {
        // Only a mapping names an account, because only a mapping states which property of the
        // connector the name was read from. A result recorded here carries no such statement, so it
        // describes the person and resolves nobody.
        Map<String, MapperResult> results = record(
                Collections.singletonMap("ssoLoginId", property("ssoLoginId", "8f3c1e4a")));

        assertNull(results.get("KeycloakApi").getLoginId());
    }

    @Test
    public void shouldStateNoSiteFromTheConnectorPath() {
        // The site is half of an identity, and a result that states one takes part in resolving the
        // account. The connector path states none.
        Map<String, MapperResult> results = record(
                Collections.singletonMap("j:email", property("j:email", "alice@example.com")));

        assertNull(results.get("KeycloakApi").getSiteKey());
    }
}
