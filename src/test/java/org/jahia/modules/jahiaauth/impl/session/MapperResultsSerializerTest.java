package org.jahia.modules.jahiaauth.impl.session;

import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.modules.jahiaauth.service.MappedPropertyInfo;
import org.jahia.modules.jahiaauth.service.MapperResult;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MapperResultsSerializerTest {

    private static MappedProperty property(String name, String value, String valueType, String format) {
        return new MappedProperty(new MappedPropertyInfo(name, valueType, format, false), value);
    }

    private static Map<String, MapperResult> oneResult(MapperResult result) {
        return Collections.singletonMap("jcrOAuthProvider", result);
    }

    @Test
    public void shouldReadBackWhatItWrote() {
        Map<String, MappedProperty> properties = new LinkedHashMap<>();
        properties.put("j:email", property("j:email", "jdoe@example.com", "email", null));
        properties.put("j:birthDate", property("j:birthDate", "1980-04-12", "date", "yyyy-MM-dd"));
        MapperResult written = new MapperResult("jdoe", "digitall", properties);

        Map<String, MapperResult> read = MapperResultsSerializer.read(MapperResultsSerializer.write(oneResult(written)));

        MapperResult result = read.get("jcrOAuthProvider");
        assertEquals("jdoe", result.getLoginId());
        assertEquals("digitall", result.getSiteKey());
        assertEquals("jdoe@example.com", result.getProperties().get("j:email").getValue());
        assertEquals("email", result.getProperties().get("j:email").getInfo().getValueType());
        assertEquals("date", result.getProperties().get("j:birthDate").getInfo().getValueType());
        assertEquals("yyyy-MM-dd", result.getProperties().get("j:birthDate").getInfo().getFormat());
    }

    @Test
    public void shouldKeepTheResultOfEveryMapper() {
        Map<String, MapperResult> results = new LinkedHashMap<>();
        results.put("jcrOAuthProvider", new MapperResult("jdoe", "digitall", Collections.emptyMap()));
        results.put("KeycloakApi", new MapperResult("jdoe", "digitall", Collections.emptyMap()));

        Map<String, MapperResult> read = MapperResultsSerializer.read(MapperResultsSerializer.write(results));

        assertEquals(2, read.size());
        assertEquals("digitall", read.get("KeycloakApi").getSiteKey());
    }

    @Test
    public void shouldCarryAResultThatStatesNoSite() {
        // A connector may record a result beside the one its mapper produced, and that result states
        // no site. It contradicts nothing, and the valve decides what to do with it.
        Map<String, MapperResult> read = MapperResultsSerializer.read(MapperResultsSerializer.write(
                oneResult(new MapperResult("jdoe", null, Collections.emptyMap()))));

        assertEquals("jdoe", read.get("jcrOAuthProvider").getLoginId());
        assertNull(read.get("jcrOAuthProvider").getSiteKey());
    }

    @Test
    public void shouldRefuseABlankSiteKey() {
        // A blank site key is a claim about the site that names no site.
        try {
            new MapperResult("jdoe", "  ", Collections.emptyMap());
            fail("A blank site key must be refused");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("names no site"));
        }
    }

    @Test
    public void shouldWriteTheSchemaVersion() throws Exception {
        String document = MapperResultsSerializer.write(
                oneResult(new MapperResult("jdoe", "digitall", Collections.emptyMap())));

        assertEquals(MapperResultsSerializer.SCHEMA_VERSION, new JSONObject(document).getInt("schemaVersion"));
    }

    @Test
    public void shouldRaiseOnADocumentOfAnotherSchemaVersion() {
        String document = "{\"schemaVersion\":99,\"results\":{\"jcrOAuthProvider\":"
                + "{\"siteKey\":\"digitall\",\"loginId\":\"jdoe\",\"properties\":{}}}}";

        try {
            MapperResultsSerializer.read(document);
            fail("A document of another schema version must be refused");
        } catch (MapperResultsFormatException e) {
            assertTrue(e.getMessage().contains("99"));
        }
    }

    @Test
    public void shouldRaiseOnAnUnreadableDocument() {
        // The caller decides what a bad document means, so the reader never guesses a partial one.
        for (String document : new String[]{"not json at all", "{\"results\":{}}",
                "{\"schemaVersion\":1,\"results\":{\"m\":{\"loginId\":\"jdoe\"}}}"}) {
            try {
                MapperResultsSerializer.read(document);
                fail("An unreadable document must be refused: " + document);
            } catch (MapperResultsFormatException e) {
                // expected
            }
        }
    }

    @Test
    public void shouldReadNoResultFromAnAbsentDocument() {
        assertTrue(MapperResultsSerializer.read("").isEmpty());
        assertTrue(MapperResultsSerializer.read(null).isEmpty());
    }

    @Test
    public void shouldCarryAResultThatResolvedNoAccount() {
        // A mapper may produce profile properties and resolve no account, so the login id is absent.
        Map<String, MapperResult> read = MapperResultsSerializer.read(MapperResultsSerializer.write(
                oneResult(new MapperResult(null, "digitall", Collections.singletonMap(
                        "j:email", property("j:email", "jdoe@example.com", "email", null))))));

        MapperResult result = read.get("jcrOAuthProvider");
        assertNull(result.getLoginId());
        assertEquals("digitall", result.getSiteKey());
        assertEquals("jdoe@example.com", result.getProperties().get("j:email").getValue());
    }

    @Test
    public void shouldWriteAValueThatIsNotAStringAsAString() {
        // One connector caches a value it never declared as a string, and a null property info with it.
        Map<String, MapperResult> read = MapperResultsSerializer.read(MapperResultsSerializer.write(
                oneResult(new MapperResult("jdoe", "digitall", Collections.singletonMap(
                        "loginCount", new MappedProperty(null, 42))))));

        MappedProperty property = read.get("jcrOAuthProvider").getProperties().get("loginCount");
        assertEquals("42", property.getValue());
        assertNull(property.getInfo().getValueType());
    }

    // An account a sign-in resolves is one this framework created for that subject.

    @Test
    public void shouldCarryTheAssertedIdentityAcrossTheSession() {
        // The valve resolves the account from this pair, and it reads the pair out of the session. A
        // document that dropped it would leave the valve resolving by name alone.
        MapperResult written = new MapperResult("jdoe", "digitall", "KeycloakApi", "8f3c1e4a",
                Collections.emptyMap());

        MapperResult read = MapperResultsSerializer.read(
                MapperResultsSerializer.write(Collections.singletonMap("jcrOAuthProvider", written)))
                .get("jcrOAuthProvider");

        assertEquals("jdoe", read.getLoginId());
        assertEquals("digitall", read.getSiteKey());
        assertEquals("KeycloakApi", read.getConnectorName());
        assertEquals("8f3c1e4a", read.getSubject());
    }

    @Test
    public void shouldCarryAResultThatStatesNoAssertedIdentity() {
        MapperResult written = new MapperResult(null, "digitall", Collections.emptyMap());

        MapperResult read = MapperResultsSerializer.read(
                MapperResultsSerializer.write(Collections.singletonMap("connectorProperties", written)))
                .get("connectorProperties");

        assertNull(read.getConnectorName());
        assertNull(read.getSubject());
    }
}
