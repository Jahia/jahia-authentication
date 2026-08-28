package org.jahia.modules.jahiaauth.impl.session;

import org.jahia.modules.jahiaauth.service.MapperResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the mapper results of one authentication on the HTTP session that produced them.
 * <p>
 * The results used to live in a cache keyed by a session id, with one implementation for a single
 * node and another one for a cluster. The session holds them now, so a cluster shares them through
 * the distributed sessions module and no second mechanism is needed.
 * <p>
 * One attribute holds the result of one mapper, and the name of the mapper is part of the attribute
 * name. Recording a result therefore writes one attribute and reads none. Holding the whole set in
 * one attribute would not survive the deployment shape this design exists for: it replicates an
 * attribute across nodes, so two callbacks on two nodes would each write a set missing the other's
 * result, and a lock on the session does not reach across nodes. Every result a session recorded has
 * to stay readable, because the valve resolves nobody when two of them state different identities.
 * <p>
 * The results are written at the callback and read on the request that follows it, so both sides sit
 * on the same session. A callback that arrives with no session cookie opens a new session, the write
 * lands on that session, and the browser carries it into the next request. Keep that order: a design
 * that wrote before the redirect to the identity provider and read after it would depend on the
 * cookie surviving a request from another site, which a browser does not guarantee.
 */
public final class SessionMapperResultsStore {

    static final String SESSION_ATTRIBUTE_PREFIX = "org.jahia.modules.jahiaauth.mapperResults.";

    /**
     * Where the results live for the duration of one request, across the sign-in.
     * <p>
     * A sign-in destroys the session and opens a new one, and it fires the LOGIN event before it
     * returns to its caller. A consumer that reads the results in that event therefore reads a
     * session that does not hold them yet. A request attribute survives the swap, because the request
     * is the same one throughout, so the valve publishes the results there before it signs the user
     * in.
     */
    static final String REQUEST_ATTRIBUTE = "org.jahia.modules.jahiaauth.mapperResultsOfThisRequest";

    private static final Logger logger = LoggerFactory.getLogger(SessionMapperResultsStore.class);

    private SessionMapperResultsStore() {
        // Utility class
    }

    /**
     * Records the result of one mapper, beside the results the session already holds.
     *
     * @param request the request the identity provider called back
     * @param mapperName the mapper that produced the result
     * @param result the result to record
     */
    public static void put(HttpServletRequest request, String mapperName, MapperResult result) {
        request.getSession().setAttribute(SESSION_ATTRIBUTE_PREFIX + mapperName,
                MapperResultsSerializer.write(Collections.singletonMap(mapperName, result)));
    }

    /**
     * @param request the request being served
     * @return every result the session holds, keyed by mapper name, empty when it holds none
     */
    public static Map<String, MapperResult> getAll(HttpServletRequest request) {
        Object carried = request.getAttribute(REQUEST_ATTRIBUTE);
        if (carried instanceof CarriedResults) {
            return ((CarriedResults) carried).results;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Collections.emptyMap();
        }
        Map<String, MapperResult> results = new LinkedHashMap<>();
        for (String name : attributeNames(session)) {
            try {
                Object stored = session.getAttribute(name);
                if (!(stored instanceof String)) {
                    // Any code holding the session can put a value here under any name. This method runs
                    // on every request of the session, before the valve knows the request concerns a
                    // sign-in, so a raise here would break the session for as long as the value lives.
                    throw new MapperResultsFormatException(
                            "The session attribute " + name + " holds no document this module wrote");
                }
                results.putAll(MapperResultsSerializer.read((String) stored));
            } catch (MapperResultsFormatException e) {
                // One unreadable result means this version cannot say what that mapper resolved, so no
                // account is resolved from any of them. Dropping only the unreadable one would be worse:
                // the results that remain would resolve an account, and the one that cannot be read is
                // exactly the one that might have named a different account and stopped the sign-in.
                // Removing them all leaves the session able to start a sign-in again, rather than
                // refusing every one until the session expires.
                logger.error("Removed the mapper results of this session, because one of them could not"
                        + " be read. No account is resolved from them, and a new sign-in starts clean.", e);
                clear(request);
                return Collections.emptyMap();
            }
        }
        return results;
    }

    /**
     * @param request the request being served
     * @param mapperName the mapper whose result is wanted
     * @return the result of that mapper, or {@code null} when the session holds none
     */
    public static MapperResult get(HttpServletRequest request, String mapperName) {
        return getAll(request).get(mapperName);
    }

    /**
     * Reads every result of this session and removes it from the session.
     * <p>
     * The caller is the request the results were recorded for, and they answer that one sign-in. What
     * the sign-in itself needs travels on the request, so nothing reads the session copy again.
     * <p>
     * The removal happens here rather than after the sign-in, because a sign-in destroys the session
     * and opens a new one. A removal that ran afterwards would run against the new session and remove
     * nothing, and it would never run at all on a sign-in that was refused. A refused sign-in is
     * exactly where the results must not stay: whatever a mapper produced would sit on the session
     * until it expired, and that includes an access token or a refresh token mapped onto a property.
     *
     * @param request the request being served
     * @return every result the session held, keyed by mapper name, empty when it held none
     */
    public static Map<String, MapperResult> take(HttpServletRequest request) {
        Map<String, MapperResult> results = getAll(request);
        clear(request);
        return results;
    }

    /**
     * Publishes the results on the request, so a reader finds them while the sign-in runs.
     *
     * @param request the request being served, before the sign-in
     * @param results the results read from the session
     */
    public static void carryAcrossSignIn(HttpServletRequest request, Map<String, MapperResult> results) {
        request.setAttribute(REQUEST_ATTRIBUTE, new CarriedResults(results));
    }

    /**
     * Stops carrying the results on the request.
     * <p>
     * The caller does this once the sign-in has run, so nothing later in the request reads a value that
     * answered a step already finished.
     *
     * @param request the request being served
     */
    public static void stopCarrying(HttpServletRequest request) {
        request.removeAttribute(REQUEST_ATTRIBUTE);
    }

    /**
     * Wraps the results carried on the request, so only this class can put them there.
     * <p>
     * The valve signs a user in with no credential, so what it reads comes from this class alone. This
     * type is package-private, in a package the bundle does not export, so no other module can build
     * one and the reader accepts nothing else.
     */
    private static final class CarriedResults {
        private final Map<String, MapperResult> results;

        CarriedResults(Map<String, MapperResult> results) {
            this.results = results;
        }
    }

    /**
     * Removes every result the session holds.
     * <p>
     * The valve calls this once a sign-in has run. The results answered that one sign-in, and a
     * consumer that reads them during it finds them on the request. Keeping them on the session would
     * leave whatever a mapper produced there for the life of the session, and that includes an access
     * token or a refresh token an administrator mapped onto a property.
     *
     * @param request the request being served
     */
    public static void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        for (String name : attributeNames(session)) {
            session.removeAttribute(name);
        }
    }

    private static List<String> attributeNames(HttpSession session) {
        List<String> names = new ArrayList<>();
        Enumeration<String> all = session.getAttributeNames();
        while (all != null && all.hasMoreElements()) {
            String name = all.nextElement();
            if (name.startsWith(SESSION_ATTRIBUTE_PREFIX)) {
                names.add(name);
            }
        }
        return names;
    }
}
