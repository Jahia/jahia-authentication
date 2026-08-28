package org.jahia.modules.jahiaauth.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LogSafeValueTest {

    private static final char CR = (char) 13;
    private static final char LF = (char) 10;
    private static final char TAB = (char) 9;

    @Test
    public void shouldKeepAnOrdinaryValueAsItIs() {
        assertEquals("jdoe@example.com", LogSafeValue.of("jdoe@example.com"));
        assertEquals("8f3c1e4a-2b71-4a0e", LogSafeValue.of("8f3c1e4a-2b71-4a0e"));
    }

    @Test
    public void shouldStopAValueFromForgingALogLine() {
        // A claim carrying a line break would otherwise write a second entry, and a reader of the log
        // cannot tell that entry from a real one.
        String claim = "alice" + LF + "2026-08-17 10:00:00: INFO [Render] - invented entry";

        String safe = LogSafeValue.of(claim);

        assertFalse(safe.indexOf(LF) >= 0);
        assertFalse(safe.indexOf(CR) >= 0);
        assertTrue(safe.startsWith("alice?2026-08-17"));
    }

    @Test
    public void shouldReplaceEveryControlCharacter() {
        assertEquals("a?b?c d", LogSafeValue.of("a" + CR + "b" + TAB + "c d"));
    }

    @Test
    public void shouldCapAValueThatWouldFloodTheLog() {
        StringBuilder flood = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            flood.append('x');
        }

        String capped = LogSafeValue.of(flood.toString());

        assertTrue(capped.length() < 200);
        assertTrue(capped.endsWith("(truncated)"));
    }

    @Test
    public void shouldNameAnAbsentValue() {
        assertEquals("null", LogSafeValue.of(null));
    }
}
