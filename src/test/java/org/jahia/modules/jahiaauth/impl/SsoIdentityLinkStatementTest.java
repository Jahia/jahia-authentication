package org.jahia.modules.jahiaauth.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The statement that resolves an account by the identity a provider asserted.
 * <p>
 * The subject is a value the identity provider chose, and it reaches a string literal of a JCR-SQL2
 * statement. {@link SubjectCheck} accepts a quote inside a subject on purpose, so the escaping here is
 * the whole defence and not a second line of one.
 */
public class SsoIdentityLinkStatementTest {

    @Test
    public void shouldReadTheLinksCarryingOnePair() {
        assertEquals("SELECT * FROM [authnt:ssoIdentityLink] WHERE [connectorName] = 'KeycloakApi'"
                + " AND [subject] = '8f3c1e4a'",
                SsoIdentityLinkServiceImpl.statement("KeycloakApi", "8f3c1e4a"));
    }

    @Test
    public void shouldEscapeAQuoteInTheSubject() {
        // A single quote closes a string literal, so the rest of the value would be read as statement
        // syntax. Doubling it keeps the whole value inside the literal.
        assertEquals("SELECT * FROM [authnt:ssoIdentityLink] WHERE [connectorName] = 'Saml'"
                + " AND [subject] = 'o''brien'",
                SsoIdentityLinkServiceImpl.statement("Saml", "o'brien"));
    }

    @Test
    public void shouldEscapeASubjectThatTriesToCloseTheLiteral() {
        String crafted = "x' OR [subject] LIKE '%";
        String statement = SsoIdentityLinkServiceImpl.statement("Saml", crafted);

        // The crafted value appears once, with every quote doubled, so it opens no clause of its own.
        assertTrue(statement.endsWith("[subject] = 'x'' OR [subject] LIKE ''%'"));
    }

    @Test
    public void shouldEscapeAQuoteInTheConnectorName() {
        // A connector name comes from configuration rather than from a request, and it reaches the same
        // context. One context, one encoder, whatever the value's provenance.
        assertEquals("SELECT * FROM [authnt:ssoIdentityLink] WHERE [connectorName] = 'o''brien'"
                + " AND [subject] = 'x'",
                SsoIdentityLinkServiceImpl.statement("o'brien", "x"));
    }
}
