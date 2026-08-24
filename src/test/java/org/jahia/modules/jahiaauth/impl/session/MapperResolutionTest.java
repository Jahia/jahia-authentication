package org.jahia.modules.jahiaauth.impl.session;

import org.jahia.modules.jahiaauth.service.JahiaAuthException;
import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.modules.jahiaauth.service.MapperConfig;
import org.jahia.modules.jahiaauth.service.Mapping;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * What the resolution of the mappings does with a configuration the ways in did not refuse.
 * <p>
 * The login id read here names the account and takes no part in resolving it, so a mapping may read it
 * from any property the connector returned. What it is held to is what a name may contain, because the
 * value reaches the repository as a path.
 * <p>
 * The identity the account is resolved by is not read here and is not a mapping. The connector names
 * the property carrying it, and {@link org.jahia.modules.jahiaauth.impl.VerifiedSubjectCheck} and
 * {@link org.jahia.modules.jahiaauth.impl.SubjectCheck} state the rules on it.
 */
public class MapperResolutionTest {

    private static Mapping mapping(String mappedProperty, String connectorProperty) {
        Mapping mapping = new Mapping();
        mapping.setMappedProperty(mappedProperty);
        mapping.setConnectorProperty(connectorProperty);
        return mapping;
    }

    private static MapperConfig config(Mapping... mappings) {
        MapperConfig config = new MapperConfig("jcrOAuthProvider", "OidcConnector", null);
        config.setMappings(Arrays.asList(mappings));
        return config;
    }

    private static Map<String, Object> returned(String... pairs) {
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.put(pairs[i], pairs[i + 1]);
        }
        return properties;
    }

    private static Map<String, Object> returned(String name, Object value) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(name, value);
        return properties;
    }

    private static Map<String, MappedProperty> resolve(MapperConfig config, Map<String, Object> returned)
            throws JahiaAuthException {
        return JahiaAuthMapperServiceImpl.resolveMappings(returned, null, config);
    }

    @Test
    public void shouldReadTheNameOfTheAccountFromTheMapping() throws JahiaAuthException {
        Map<String, MappedProperty> result = resolve(config(mapping("ssoLoginId", "sub")),
                returned("sub", "8f3c1e4a"));

        assertEquals("8f3c1e4a", result.get("ssoLoginId").getValue());
    }

    @Test
    public void shouldReadTheNameOfTheAccountFromAnyPropertyTheConnectorReturned() throws JahiaAuthException {
        // The name is chosen by the deployment and takes no part in resolving the account, so the
        // property it is read from decides the name and nothing else.
        Map<String, MappedProperty> result = resolve(config(mapping("ssoLoginId", "email")),
                returned("email", "alice@example.com"));

        assertEquals("alice@example.com", result.get("ssoLoginId").getValue());
    }

    @Test
    public void shouldReadTheProfilePropertiesBesideTheName() throws JahiaAuthException {
        Map<String, MappedProperty> result = resolve(
                config(mapping("ssoLoginId", "sub"), mapping("j:firstName", "given_name")),
                returned("sub", "8f3c1e4a", "given_name", "Alice"));

        assertEquals("8f3c1e4a", result.get("ssoLoginId").getValue());
        assertEquals("Alice", result.get("j:firstName").getValue());
    }

    @Test
    public void shouldReadTheNameWhenTheMappingIsWrittenAsAnExpression() throws JahiaAuthException {
        Map<String, MappedProperty> result = resolve(config(mapping("ssoLoginId", "$.sub")),
                returned("$.sub", "8f3c1e4a"));

        assertEquals("8f3c1e4a", result.get("ssoLoginId").getValue());
    }

    // A login id is validated as an account name before it reaches the repository.

    @Test
    public void shouldNameNoAccountFromAMultiValuedClaim() throws JahiaAuthException {
        // A protocol may return a claim more than once, and the two values may disagree. Joining them
        // invents a name that neither states.
        Map<String, MappedProperty> result = resolve(config(mapping("ssoLoginId", "sub")),
                returned("sub", Arrays.asList("alice", "alice")));
        assertFalse(result.containsKey("ssoLoginId"));
    }

    @Test
    public void shouldNameNoAccountFromANameCarryingAPathSeparator() throws JahiaAuthException {
        // The account name becomes a repository path, and a separator inside it selects another place
        // in the repository than the one the caller named.
        Map<String, MappedProperty> result = resolve(config(mapping("ssoLoginId", "sub")),
                returned("sub", "alice/elsewhere"));
        assertFalse(result.containsKey("ssoLoginId"));
    }

    @Test
    public void shouldNameNoAccountFromANameOpeningAnIdentifierSegment() throws JahiaAuthException {
        // A leading bracket makes the repository read the whole name as a node identifier rather than
        // as a name.
        Map<String, MappedProperty> result = resolve(config(mapping("ssoLoginId", "sub")),
                returned("sub", "[alice]"));
        assertFalse(result.containsKey("ssoLoginId"));
    }

    @Test
    public void shouldKeepTheProfilePropertiesOfARefusedName() throws JahiaAuthException {
        // A profile property names no account, so only the login id goes. The result then names no
        // account, and the valve signs nobody in.
        Map<String, MappedProperty> result = resolve(
                config(mapping("ssoLoginId", "sub"), mapping("j:firstName", "given_name")),
                returned("sub", "alice/elsewhere", "given_name", "Alice"));

        assertFalse(result.containsKey("ssoLoginId"));
        assertEquals("Alice", result.get("j:firstName").getValue());
    }

    @Test
    public void shouldNameAnAccountFromAnOrdinaryName() throws JahiaAuthException {
        // The values a real provider returns are subject identifiers, and they have to keep resolving.
        for (String name : new String[] {"alice", "8f3c1e4a-2b71-4c9f-9c62-1d0e4f5a6b7c", "alice.smith",
                "alice_smith", "alice-smith", "alice@example.com"}) {
            Map<String, MappedProperty> result = resolve(config(mapping("ssoLoginId", "sub")),
                    returned("sub", name));
            assertEquals(name, result.get("ssoLoginId").getValue());
        }
    }

    @Test
    public void shouldKeepAMultiValuedProfileProperty() throws JahiaAuthException {
        // A profile property names no account, so several values are its own business.
        Map<String, MappedProperty> result = resolve(config(mapping("j:organization", "groups")),
                returned("groups", Arrays.asList("sales", "support")));
        assertTrue(result.containsKey("j:organization"));
    }
}
