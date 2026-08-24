package org.jahia.modules.jahiaauth.impl;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * What a subject may be.
 * <p>
 * A subject names no node and no path, so the set is wide on purpose: the statement that reads it back
 * escapes it for the context it is printed in, and this check is not what makes that statement safe.
 * The cases below state that widening it breaks nothing, which is the difference from
 * {@link AccountNameCheck}.
 */
public class SubjectCheckTest {

    @Test
    public void shouldAcceptTheSubjectsRealProvidersAssert() {
        assertNull(SubjectCheck.refusalReason("8f3c1e4a-2b71-4c9f-9c62-1d0e4f5a6b7c"));
        assertNull(SubjectCheck.refusalReason("alice@example.com"));
        assertNull(SubjectCheck.refusalReason("CN=Alice Smith,OU=People,DC=example,DC=com"));
        assertNull(SubjectCheck.refusalReason("110248495921238986420"));
    }

    @Test
    public void shouldAcceptASubjectHoldingWhatAnAccountNameRefuses() {
        // A subject reaches a statement and not a path, so the characters an account name refuses are
        // recorded as they arrive. A check that refused them would be the thing the statement rests on.
        assertNull(SubjectCheck.refusalReason("alice/elsewhere"));
        assertNull(SubjectCheck.refusalReason("[alice]"));
        assertNull(SubjectCheck.refusalReason("o'brien"));
        assertNull(SubjectCheck.refusalReason("alice smith"));
    }

    @Test
    public void shouldRefuseNoValue() {
        assertNotNull(SubjectCheck.refusalReason(null));
    }

    @Test
    public void shouldRefuseABlankValue() {
        assertNotNull(SubjectCheck.refusalReason(""));
        assertNotNull(SubjectCheck.refusalReason("   "));
    }

    @Test
    public void shouldRefuseSeveralValues() {
        // Two values describe two identities, and joining them describes a third nobody asserted.
        assertNotNull(SubjectCheck.refusalReason(Arrays.asList("alice", "bob")));
        assertNotNull(SubjectCheck.refusalReason(Collections.singletonList("alice")));
        assertNotNull(SubjectCheck.refusalReason(new String[] {"alice"}));
    }

    @Test
    public void shouldRefuseAValueTooLongToBeAnIdentifier() {
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 513; i++) {
            tooLong.append('a');
        }
        assertNotNull(SubjectCheck.refusalReason(tooLong.toString()));
        assertNull(SubjectCheck.refusalReason(tooLong.substring(0, 512)));
    }
}
