package org.jahia.modules.jahiaauth.impl.session;

import org.jahia.modules.jahiaauth.service.MapperResult;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SessionMapperResultsStoreTest {

    private Map<String, Object> sessionAttributes;
    private Map<String, Object> requestAttributes;
    private HttpServletRequest request;

    /**
     * The servlet API is an interface, and only a handful of its methods matter here, so a proxy over
     * two maps stands in for a container. It keeps the test free of a mocking dependency.
     */
    private static Object proxy(Class<?> type, InvocationHandler handler) {
        return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    @Before
    public void setUp() {
        sessionAttributes = new HashMap<>();
        requestAttributes = new HashMap<>();
        HttpSession session = (HttpSession) proxy(HttpSession.class, (p, method, args) -> {
            switch (method.getName()) {
                case "getAttribute": return sessionAttributes.get((String) args[0]);
                case "setAttribute": return sessionAttributes.put((String) args[0], args[1]);
                case "removeAttribute": return sessionAttributes.remove((String) args[0]);
                // The store enumerates the names, because one attribute holds one mapper's result.
                case "getAttributeNames": return Collections.enumeration(new ArrayList<>(sessionAttributes.keySet()));
                default: return null;
            }
        });
        request = (HttpServletRequest) proxy(HttpServletRequest.class, (p, method, args) -> {
            switch (method.getName()) {
                case "getSession": return session;
                case "getAttribute": return requestAttributes.get((String) args[0]);
                case "setAttribute": return requestAttributes.put((String) args[0], args[1]);
                default: return null;
            }
        });
    }

    private static MapperResult result(String loginId, String siteKey) {
        return new MapperResult(loginId, siteKey, Collections.emptyMap());
    }

    @Test
    public void shouldReadBackEveryResultItRecorded() {
        SessionMapperResultsStore.put(request, "jcrOAuthProvider", result("jdoe", "digitall"));
        SessionMapperResultsStore.put(request, "KeycloakApi", result("jdoe", null));

        Map<String, MapperResult> read = SessionMapperResultsStore.getAll(request);

        assertEquals(2, read.size());
        assertEquals("digitall", read.get("jcrOAuthProvider").getSiteKey());
        assertNull(read.get("KeycloakApi").getSiteKey());
    }

    @Test
    public void shouldWriteOneAttributePerMapperAndReadNone() {
        // Recording a result must not read the others, because a read, a change and a write cannot be
        // made safe on an attribute a cluster replicates.
        SessionMapperResultsStore.put(request, "jcrOAuthProvider", result("jdoe", "digitall"));
        SessionMapperResultsStore.put(request, "KeycloakApi", result("jdoe", null));

        assertEquals(2, sessionAttributes.size());
        assertTrue(sessionAttributes.containsKey(SessionMapperResultsStore.SESSION_ATTRIBUTE_PREFIX + "KeycloakApi"));
    }

    @Test
    public void shouldResolveNoAccountAndRecoverWhenOneResultCannotBeRead() {
        SessionMapperResultsStore.put(request, "jcrOAuthProvider", result("jdoe", "digitall"));
        sessionAttributes.put(SessionMapperResultsStore.SESSION_ATTRIBUTE_PREFIX + "broken", "{\"schemaVersion\":99}");

        // No account resolves from any of them, because one of them says nothing this version can read.
        assertTrue(SessionMapperResultsStore.getAll(request).isEmpty());
        // And the session can start a sign-in again, rather than refusing every one until it expires.
        assertTrue(sessionAttributes.isEmpty());
    }

    @Test
    public void shouldResolveNoAccountWhenAnAttributeHoldsSomethingElse() {
        // Anything running in the container can put a value on this session under any name. A value that
        // is not a document this framework wrote must resolve no account, and it must not raise: the
        // valve reads the results on every request of the session, before it knows the request concerns
        // a sign-in, so a raise here breaks the session for as long as the value lives.
        SessionMapperResultsStore.put(request, "jcrOAuthProvider", result("jdoe", "digitall"));
        sessionAttributes.put(SessionMapperResultsStore.SESSION_ATTRIBUTE_PREFIX + "other", 42);

        assertTrue(SessionMapperResultsStore.getAll(request).isEmpty());
        assertTrue(sessionAttributes.isEmpty());
    }

    @Test
    public void shouldFindTheResultsWhileTheSignInRuns() {
        // A sign-in destroys the session and fires the LOGIN event before it returns. A consumer
        // reading in that event must still find the results, so the valve carries them on the request.
        Map<String, MapperResult> results = SessionMapperResultsStore.getAll(request);
        SessionMapperResultsStore.put(request, "jcrOAuthProvider", result("jdoe", "digitall"));
        results = SessionMapperResultsStore.getAll(request);

        SessionMapperResultsStore.carryAcrossSignIn(request, results);
        sessionAttributes.clear();  // what authenticate() does to the session

        Map<String, MapperResult> duringSignIn = SessionMapperResultsStore.getAll(request);
        assertEquals("jdoe", duringSignIn.get("jcrOAuthProvider").getLoginId());
    }

    @Test
    public void shouldHoldNothingOnceTheSignInHasRun() {
        SessionMapperResultsStore.put(request, "jcrOAuthProvider", result("jdoe", "digitall"));

        SessionMapperResultsStore.clear(request);

        assertTrue(sessionAttributes.isEmpty());
        assertTrue(SessionMapperResultsStore.getAll(request).isEmpty());
    }

    @Test
    public void shouldHandOverTheResultsAndStopHoldingThem() {
        SessionMapperResultsStore.put(request, "jcrOAuthProvider", result("jdoe", "digitall"));
        SessionMapperResultsStore.put(request, "samlMapper", result("jdoe", "digitall"));

        Map<String, MapperResult> taken = SessionMapperResultsStore.take(request);

        assertEquals(2, taken.size());
        assertTrue(sessionAttributes.isEmpty());
    }

    @Test
    public void shouldHandOverNothingTwice() {
        // A sign-in that was refused must not leave the results behind for a later request to use. What
        // a mapper produced can include an access token or a refresh token mapped onto a property.
        SessionMapperResultsStore.put(request, "jcrOAuthProvider", result("jdoe", "digitall"));

        SessionMapperResultsStore.take(request);

        assertTrue(SessionMapperResultsStore.take(request).isEmpty());
    }

    @Test
    public void shouldHoldNothingBeforeAnyResultIsRecorded() {
        assertTrue(SessionMapperResultsStore.getAll(request).isEmpty());
        assertNull(SessionMapperResultsStore.get(request, "jcrOAuthProvider"));
    }
}
