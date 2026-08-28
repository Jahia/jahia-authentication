package org.jahia.modules.jahiaauth.impl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The identity an account is resolved by comes from the property the connector declares.
 * <p>
 * The rule is stated over the connector rather than over a mapping. A configuration names the property
 * a mapping reads, and it does not name the property carrying the asserted identity: the framework
 * reads what the connector declares. There is therefore no configuration left to refuse, and what is
 * refused is a connector that declares nothing.
 */
public class VerifiedSubjectCheckTest {

    @Test
    public void shouldAcceptAConnectorThatDeclaresItsVerifiedSubject() {
        assertNull(VerifiedSubjectCheck.refusalReason("KeycloakApi", "sub"));
        assertNull(VerifiedSubjectCheck.refusalReason("Saml", "nameID"));
        assertNull(VerifiedSubjectCheck.refusalReason("GoogleApi20", "id"));
    }

    @Test
    public void shouldRefuseAConnectorThatDeclaresNone() {
        assertNotNull(VerifiedSubjectCheck.refusalReason("HomeGrownConnector", null));
    }

    @Test
    public void shouldRefuseAConnectorThatDeclaresABlankProperty() {
        // A property name of spaces reads nothing, and a connector that returns it declares nothing.
        assertNotNull(VerifiedSubjectCheck.refusalReason("HomeGrownConnector", "   "));
    }

    @Test
    public void shouldSayWhichConnectorAndWhatToDo() {
        String reason = VerifiedSubjectCheck.refusalReason("HomeGrownConnector", null);
        assertTrue(reason.contains("HomeGrownConnector"));
        assertTrue(reason.contains("getVerifiedSubjectProperty"));
    }
}
