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
        // several connectors may have cached a result for the same session
        Map<String, Map<String, MappedProperty>> allMapperResult = new LinkedHashMap<>();
        allMapperResult.put("profileMapper", mapperResult(null, "otherSite"));
        allMapperResult.put("jcrOAuthProvider", mapperResult("jdoe", "digitall"));

        SSOValve.SsoIdentity identity = SSOValve.findIdentity(allMapperResult);

        assertEquals("jdoe", identity.getUserId());
        assertEquals("digitall", identity.getSiteKey());
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
