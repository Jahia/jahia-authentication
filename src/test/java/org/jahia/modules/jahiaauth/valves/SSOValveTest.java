package org.jahia.modules.jahiaauth.valves;

import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.modules.jahiaauth.service.MappedPropertyInfo;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SSOValveTest {

    private static MappedProperty property(String name, String value) {
        return new MappedProperty(new MappedPropertyInfo(name, "string", null, false), value);
    }

    /** The results two connectors cached for one and the same session. */
    private static Map<String, Map<String, MappedProperty>> bothOf(Map<String, MappedProperty> oauth,
                                                                   Map<String, MappedProperty> saml) {
        Map<String, Map<String, MappedProperty>> allMapperResult = new LinkedHashMap<>();
        allMapperResult.put("jcrOAuthProvider", oauth);
        allMapperResult.put("samlMapper", saml);
        return allMapperResult;
    }

    private static Map<String, MappedProperty> mapperResult(String loginId, String siteKey) {
        Map<String, MappedProperty> result = new HashMap<>();
        if (loginId != null) {
            result.put(JahiaAuthConstants.SSO_LOGIN, property(JahiaAuthConstants.SSO_LOGIN, loginId));
        }
        if (siteKey != null) {
            result.put(JahiaAuthConstants.SITE_KEY, property(JahiaAuthConstants.SITE_KEY, siteKey));
        }
        return result;
    }

    @Test
    public void shouldReadTheLoginIdAndItsSiteKeyFromTheSameMapperResult() {
        Map<String, Map<String, MappedProperty>> allMapperResult = new LinkedHashMap<>();
        allMapperResult.put("jcrOAuthProvider", mapperResult("jdoe", "digitall"));

        SSOValve.SsoIdentity identity = SSOValve.findIdentity(allMapperResult);

        assertEquals("jdoe", identity.getUserId());
        assertEquals("digitall", identity.getSiteKey());
    }

    @Test
    public void shouldPairTheLoginIdWithTheSiteKeyOfItsOwnMapperResult() {
        // Several connectors may have cached a result for the same session, and only some of them carry
        // a login id. Exactly one does here, so the site key of the other one is never the answer.
        Map<String, Map<String, MappedProperty>> allMapperResult = new LinkedHashMap<>();
        allMapperResult.put("profileMapper", mapperResult(null, "otherSite"));
        allMapperResult.put("jcrOAuthProvider", mapperResult("jdoe", "digitall"));

        SSOValve.SsoIdentity identity = SSOValve.findIdentity(allMapperResult);

        assertEquals("jdoe", identity.getUserId());
        assertEquals("digitall", identity.getSiteKey());
    }

    @Test
    public void shouldFindTheIdentitySeveralMapperResultsAgreeOn() {
        // one cached result per mapper, both fed by the same connector: same account, same site
        Map<String, Map<String, MappedProperty>> allMapperResult = new LinkedHashMap<>();
        allMapperResult.put("jcrOAuthProvider", mapperResult("jdoe", "digitall"));
        allMapperResult.put("profileMapper", mapperResult("jdoe", "digitall"));

        SSOValve.SsoIdentity identity = SSOValve.findIdentity(allMapperResult);

        assertEquals("jdoe", identity.getUserId());
        assertEquals("digitall", identity.getSiteKey());
    }

    @Test
    public void shouldFindNoIdentityWhenMapperResultsDisagree() {
        // the pick used to be undefined whichever way the results differed; pairing the site with the
        // login id is what makes the outcome differ rather than just the route to it. Each shape below
        // is a session two connectors describe differently, and none of them resolves an account.
        Map<String, MappedProperty> reference = mapperResult("jdoe", "digitall");

        assertNull("two results naming different accounts",
                SSOValve.findIdentity(bothOf(reference, mapperResult("asmith", "digitall"))));
        assertNull("two results naming one account on different sites",
                SSOValve.findIdentity(bothOf(reference, mapperResult("jdoe", "otherSite"))));
        assertNull("one of the two results carrying no site key",
                SSOValve.findIdentity(bothOf(reference, mapperResult("jdoe", null))));
    }

    @Test
    public void shouldReportNoSiteKeyWhenTheMapperResultHoldsNone() {
        Map<String, Map<String, MappedProperty>> allMapperResult = new LinkedHashMap<>();
        allMapperResult.put("jcrOAuthProvider", mapperResult("jdoe", null));

        SSOValve.SsoIdentity identity = SSOValve.findIdentity(allMapperResult);

        assertEquals("jdoe", identity.getUserId());
        assertNull(identity.getSiteKey());
    }

    @Test
    public void shouldFindNoIdentityWhenNoMapperResultHoldsALoginId() {
        Map<String, Map<String, MappedProperty>> allMapperResult = new LinkedHashMap<>();
        allMapperResult.put("profileMapper", mapperResult(null, "digitall"));

        assertNull(SSOValve.findIdentity(allMapperResult));
    }

    @Test
    public void shouldFindNoIdentityWhenNoMapperResultIsCached() {
        assertNull(SSOValve.findIdentity(new HashMap<>()));
    }

    @Test
    public void shouldFindNoIdentityWhenTheLoginIdHasNoValue() {
        Map<String, MappedProperty> result = new HashMap<>();
        result.put(JahiaAuthConstants.SSO_LOGIN, property(JahiaAuthConstants.SSO_LOGIN, null));
        Map<String, Map<String, MappedProperty>> allMapperResult = new LinkedHashMap<>();
        allMapperResult.put("jcrOAuthProvider", result);

        assertNull(SSOValve.findIdentity(allMapperResult));
    }
}
