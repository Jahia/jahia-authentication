package org.jahia.modules.jahiaauth.impl.session;

import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.modules.jahiaauth.service.MappedPropertyInfo;
import org.jahia.modules.jahiaauth.service.MapperResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads and writes the mapper results of one session as a JSON document.
 * <p>
 * The document is a string, so it carries no class identity. It survives a module upgrade, a change
 * of major version and a transfer between cluster nodes, and no class of this module is ever
 * deserialized to read it. The framework parses the document itself and hands typed objects to its
 * callers, so a consumer never parses JSON either.
 * <p>
 * A document this class cannot read raises {@link MapperResultsFormatException}. It never yields a
 * partial reading, because the caller decides what a bad document means. A reader refuses the
 * sign-in that depends on it, and a writer refuses to replace it. Both directions are safe. A reader
 * that guessed would sign in an account it did not resolve, and a writer that replaced a document it
 * could not read would turn two contradicting results into one agreeing result.
 *
 * <pre>
 * {
 *   "schemaVersion": 1,
 *   "results": {
 *     "jcrOAuthProvider": {
 *       "siteKey": "digitall",
 *       "loginId": "jdoe",
 *       "connectorName": "KeycloakApi",
 *       "subject": "8f3c1e4a-...",
 *       "properties": {
 *         "j:email": { "value": "jdoe@example.com", "valueType": "email" }
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public final class MapperResultsSerializer {

    /**
     * Version 2 carries the connector and the subject a result states. A reader refuses a document of
     * another version rather than reading it partially, so a document this version did not write
     * resolves no account.
     */
    static final int SCHEMA_VERSION = 2;

    private static final String SCHEMA_VERSION_FIELD = "schemaVersion";
    private static final String RESULTS = "results";
    private static final String SITE_KEY = "siteKey";
    private static final String LOGIN_ID = "loginId";
    private static final String CONNECTOR_NAME = "connectorName";
    private static final String SUBJECT = "subject";
    private static final String PROPERTIES = "properties";
    private static final String VALUE = "value";
    private static final String VALUE_TYPE = "valueType";
    private static final String FORMAT = "format";

    private MapperResultsSerializer() {
        // Utility class
    }

    /**
     * @param results the results to write, keyed by mapper name
     * @return the document, never {@code null}
     */
    public static String write(Map<String, MapperResult> results) {
        JSONObject document = new JSONObject();
        JSONObject written = new JSONObject();
        try {
            for (Map.Entry<String, MapperResult> entry : results.entrySet()) {
                written.put(entry.getKey(), writeResult(entry.getValue()));
            }
            document.put(SCHEMA_VERSION_FIELD, SCHEMA_VERSION);
            document.put(RESULTS, written);
        } catch (JSONException e) {
            // A JSONObject refuses a null name and a non-finite number, and this method writes neither.
            throw new IllegalStateException("Could not write the mapper results", e);
        }
        return document.toString();
    }

    /**
     * @param document the document to read, may be {@code null}
     * @return the results, keyed by mapper name, empty when the document is absent
     * @throws MapperResultsFormatException when the document is present and cannot be read
     */
    public static Map<String, MapperResult> read(String document) {
        if (document == null || document.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            JSONObject parsed = new JSONObject(document);
            int version = parsed.getInt(SCHEMA_VERSION_FIELD);
            if (version != SCHEMA_VERSION) {
                throw new MapperResultsFormatException("The document carries schema version " + version
                        + ", and this module reads version " + SCHEMA_VERSION);
            }
            return readResults(parsed.getJSONObject(RESULTS));
        } catch (JSONException | IllegalArgumentException e) {
            throw new MapperResultsFormatException("The document could not be read", e);
        }
    }

    private static JSONObject writeResult(MapperResult result) throws JSONException {
        JSONObject properties = new JSONObject();
        for (Map.Entry<String, MappedProperty> entry : result.getProperties().entrySet()) {
            properties.put(entry.getKey(), writeProperty(entry.getValue()));
        }
        JSONObject written = new JSONObject();
        written.put(SITE_KEY, result.getSiteKey());
        if (result.getLoginId() != null) {
            written.put(LOGIN_ID, result.getLoginId());
        }
        if (result.getConnectorName() != null) {
            written.put(CONNECTOR_NAME, result.getConnectorName());
        }
        if (result.getSubject() != null) {
            written.put(SUBJECT, result.getSubject());
        }
        written.put(PROPERTIES, properties);
        return written;
    }

    private static JSONObject writeProperty(MappedProperty property) throws JSONException {
        JSONObject written = new JSONObject();
        // Every value is written as a string, which is what the mapper produced: getMapperResults
        // applies String.valueOf to each property it reads from the connector.
        written.put(VALUE, property.getValue() == null ? JSONObject.NULL : String.valueOf(property.getValue()));
        MappedPropertyInfo info = property.getInfo();
        if (info != null) {
            if (info.getValueType() != null) {
                written.put(VALUE_TYPE, info.getValueType());
            }
            if (info.getFormat() != null) {
                written.put(FORMAT, info.getFormat());
            }
        }
        return written;
    }

    private static Map<String, MapperResult> readResults(JSONObject results) throws JSONException {
        Map<String, MapperResult> read = new LinkedHashMap<>();
        JSONArray mapperNames = results.names();
        if (mapperNames == null) {
            return read;
        }
        for (int i = 0; i < mapperNames.length(); i++) {
            String mapperName = mapperNames.getString(i);
            JSONObject result = results.getJSONObject(mapperName);
            read.put(mapperName, new MapperResult(
                    result.optString(LOGIN_ID, null),
                    result.optString(SITE_KEY, null),
                    result.optString(CONNECTOR_NAME, null),
                    result.optString(SUBJECT, null),
                    readProperties(result.getJSONObject(PROPERTIES))));
        }
        return read;
    }

    private static Map<String, MappedProperty> readProperties(JSONObject properties) throws JSONException {
        Map<String, MappedProperty> read = new LinkedHashMap<>();
        JSONArray names = properties.names();
        if (names == null) {
            return read;
        }
        for (int i = 0; i < names.length(); i++) {
            String name = names.getString(i);
            JSONObject property = properties.getJSONObject(name);
            MappedPropertyInfo info = new MappedPropertyInfo(name,
                    property.optString(VALUE_TYPE, null), property.optString(FORMAT, null), false);
            read.put(name, new MappedProperty(info, property.isNull(VALUE) ? null : property.getString(VALUE)));
        }
        return read;
    }
}
