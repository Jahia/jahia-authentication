package org.jahia.modules.jahiaauth.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServiceFilterTest {

    @Test
    public void shouldSelectTheServiceThatCarriesTheName() {
        assertEquals("(connectorServiceName=OidcConnector)",
                ServiceFilter.byName("connectorServiceName", "OidcConnector"));
    }

    @Test
    public void shouldStopANameFromSelectingEveryService() {
        // A request body decides this name. Unescaped, a single star returns whichever connector the
        // container answers first, and the caller then reads the rules of a connector it did not name.
        assertEquals("(connectorServiceName=\\2a)", ServiceFilter.byName("connectorServiceName", "*"));
    }

    @Test
    public void shouldStopANameFromChangingTheFilter() {
        assertEquals("(connectorServiceName=a\\29\\28b)",
                ServiceFilter.byName("connectorServiceName", "a)(b"));
        assertEquals("(connectorServiceName=a\\5cb)", ServiceFilter.byName("connectorServiceName", "a\\b"));
    }

    @Test
    public void shouldMatchNothingForAnAbsentName() {
        assertEquals("(connectorServiceName=)", ServiceFilter.byName("connectorServiceName", null));
    }
}
