package org.jahia.modules.jahiaauth.valves;

import org.jahia.modules.jahiaauth.service.MapperResult;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class SSOValveTest {

    /**
     * A result stating a whole identity: the account, its site, and the pair the account is resolved
     * by. The subject is derived from the account name so that two accounts differ in both.
     */
    private static MapperResult result(String loginId, String siteKey) {
        return new MapperResult(loginId, siteKey, "KeycloakApi",
                loginId == null ? null : "subject-of-" + loginId, Collections.emptyMap());
    }

    /** A result stating half of an identity: an account name, and no identity to resolve it by. */
    private static MapperResult resultWithoutSubject(String loginId, String siteKey) {
        return new MapperResult(loginId, siteKey, Collections.emptyMap());
    }

    private static MapperResult result(String loginId, String siteKey, String connectorName, String subject) {
        return new MapperResult(loginId, siteKey, connectorName, subject, Collections.emptyMap());
    }

    /** The results two connectors recorded on one and the same session. */
    private static Map<String, MapperResult> bothOf(MapperResult oauth, MapperResult saml) {
        Map<String, MapperResult> results = new LinkedHashMap<>();
        results.put("jcrOAuthProvider", oauth);
        results.put("samlMapper", saml);
        return results;
    }

    @Test
    public void shouldReadTheLoginIdAndItsSiteKeyFromTheSameResult() {
        SSOValve.SsoIdentity identity = SSOValve.findIdentity(
                Collections.singletonMap("jcrOAuthProvider", result("jdoe", "digitall")));

        assertEquals("jdoe", identity.getUserId());
        assertEquals("digitall", identity.getSiteKey());
    }

    @Test
    public void shouldIgnoreAResultThatNamesNoAccount() {
        // A mapper may produce profile properties and resolve no account, so it names none. The site
        // key of that result is never the answer.
        SSOValve.SsoIdentity identity = SSOValve.findIdentity(
                bothOf(result(null, "otherSite"), result("jdoe", "digitall")));

        assertEquals("jdoe", identity.getUserId());
        assertEquals("digitall", identity.getSiteKey());
    }

    @Test
    public void shouldFindTheIdentitySeveralResultsAgreeOn() {
        // One result per mapper, both fed by the same connector: same account, same site.
        SSOValve.SsoIdentity identity = SSOValve.findIdentity(
                bothOf(result("jdoe", "digitall"), result("jdoe", "digitall")));

        assertEquals("jdoe", identity.getUserId());
        assertEquals("digitall", identity.getSiteKey());
    }

    @Test
    public void shouldFindNoIdentityWhenResultsNameDifferentAccounts() {
        assertNull(SSOValve.findIdentity(bothOf(result("jdoe", "digitall"), result("asmith", "digitall"))));
    }

    @Test
    public void shouldFindNoIdentityWhenResultsNameOneAccountOnTwoSites() {
        assertNull(SSOValve.findIdentity(bothOf(result("jdoe", "digitall"), result("jdoe", "otherSite"))));
    }

    @Test
    public void shouldFindNoIdentityWhenNoResultNamesAnAccount() {
        assertNull(SSOValve.findIdentity(bothOf(result(null, "digitall"), result(null, "otherSite"))));
    }

    // An account a sign-in resolves is one this framework created for that subject.

    @Test
    public void shouldFindNoIdentityWhenAResultStatesNoAssertedSubject() {
        // The name of an account is chosen by the deployment and takes no part in resolving it. A
        // result carrying a name and no asserted identity therefore describes nothing to resolve.
        assertNull(SSOValve.findIdentity(Collections.singletonMap("jcrOAuthProvider",
                resultWithoutSubject("jdoe", "digitall"))));
    }

    @Test
    public void shouldFindNoIdentityWhenOneResultOfTwoStatesNoAssertedSubject() {
        assertNull(SSOValve.findIdentity(bothOf(result("jdoe", "digitall"),
                resultWithoutSubject("jdoe", "digitall"))));
    }

    @Test
    public void shouldFindNoIdentityWhenAResultStatesASubjectAndNamesNoAccount() {
        // Half an identity. The account to name comes from no result.
        assertNull(SSOValve.findIdentity(Collections.singletonMap("jcrOAuthProvider",
                result(null, "digitall", "KeycloakApi", "8f3c1e4a"))));
    }

    @Test
    public void shouldFindNoIdentityWhenResultsStateDifferentSubjects() {
        // One name, two asserted identities. Each one resolves an account of its own, so neither is the
        // account of this sign-in.
        assertNull(SSOValve.findIdentity(bothOf(result("jdoe", "digitall", "KeycloakApi", "8f3c1e4a"),
                result("jdoe", "digitall", "KeycloakApi", "1a2b3c4d"))));
    }

    @Test
    public void shouldFindNoIdentityWhenResultsNameDifferentConnectors() {
        // A subject is stored beside the connector that asserted it, so the same string asserted by two
        // connectors states two identities.
        assertNull(SSOValve.findIdentity(bothOf(result("jdoe", "digitall", "KeycloakApi", "8f3c1e4a"),
                result("jdoe", "digitall", "Saml", "8f3c1e4a"))));
    }

    @Test
    public void shouldReadTheAssertedIdentityFromTheSameResultAsTheAccount() {
        SSOValve.SsoIdentity identity = SSOValve.findIdentity(Collections.singletonMap("jcrOAuthProvider",
                result("jdoe", "digitall", "KeycloakApi", "8f3c1e4a")));

        assertEquals("jdoe", identity.getUserId());
        assertEquals("digitall", identity.getSiteKey());
        assertEquals("KeycloakApi", identity.getConnectorName());
        assertEquals("8f3c1e4a", identity.getSubject());
    }

    @Test
    public void shouldNotNameTheSubjectInWhatReachesALogLine() {
        // The value reaches a log line, and the subject is what resolves an account.
        String described = SSOValve.findIdentity(Collections.singletonMap("jcrOAuthProvider",
                result("jdoe", "digitall", "KeycloakApi", "8f3c1e4a"))).toString();

        assertFalse(described.contains("8f3c1e4a"));
    }

    // One sign-in resolves one login id and one site key, both from the same result.

    @Test
    public void shouldFindNoIdentityWhenAResultNamesAnAccountWithoutASite() {
        // An account is resolved against a site, so a result naming an account and no site describes
        // half an identity. Reading the other half from a second result assembles an identity that no
        // result states, and the halves would be picked by the iteration order of an unordered map.
        assertNull(SSOValve.findIdentity(bothOf(result("jdoe", "digitall"), result("jdoe", null))));
        assertNull(SSOValve.findIdentity(bothOf(result("jdoe", null), result("jdoe", "digitall"))));
    }

    @Test
    public void shouldFindNoIdentityWhenNoResultStatesASite() {
        assertNull(SSOValve.findIdentity(bothOf(result("jdoe", null), result("jdoe", null))));
    }

    @Test
    public void shouldFindNoIdentityFromASingleResultStatingNoSite() {
        assertNull(SSOValve.findIdentity(Collections.singletonMap("jcrOAuthProvider", result("jdoe", null))));
    }

    @Test
    public void shouldFindNoIdentityWhenTheSessionHoldsNoResult() {
        assertNull(SSOValve.findIdentity(Collections.emptyMap()));
    }
}
