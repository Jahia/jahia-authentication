package org.jahia.modules.jahiaauth.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What one mapper resolved for one authentication: the account to sign in, the site it is resolved
 * against, and the profile properties the mapper produced.
 * <p>
 * The site key comes from the configuration of the connector that ran the mapper, never from the
 * request. It is mandatory, because the account is resolved against it.
 * <p>
 * The login id and the site key are fields of their own. Earlier versions kept both inside the
 * property map, under the reserved names {@code ssoLoginId} and {@code siteKey}, and every consumer
 * had to filter those two names out of the profile properties.
 * <p>
 * The login id names the account, and it does not resolve it. The pair of the connector and the
 * subject is what resolves the account, so a result carries that pair beside the name. A result
 * naming an account and stating no subject resolves nothing, because the name takes no part in the
 * resolution.
 */
public class MapperResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String loginId;
    private final String siteKey;
    private final String connectorName;
    private final String subject;
    private final Map<String, MappedProperty> properties;

    /**
     * A result that states no identity, which is what a record of connector properties is.
     *
     * @param loginId the account the mapper resolved, may be {@code null} when the mapper resolves none
     * @param siteKey the site the account is resolved against, {@code null} when the result states none
     * @param properties the profile properties, without the login id and the site key
     * @throws IllegalArgumentException if {@code siteKey} is blank
     */
    public MapperResult(String loginId, String siteKey, Map<String, MappedProperty> properties) {
        this(loginId, siteKey, null, null, properties);
    }

    /**
     * @param loginId the account the mapper resolved, may be {@code null} when the mapper resolves none
     * @param siteKey the site the account is resolved against, {@code null} when the result states none
     * @param connectorName the connector whose identity provider asserted the subject, {@code null}
     *        when the result states no asserted identity
     * @param subject the subject the identity provider asserted, {@code null} when it states none
     * @param properties the profile properties, without the login id and the site key
     * @throws IllegalArgumentException if {@code siteKey} is blank
     */
    public MapperResult(String loginId, String siteKey, String connectorName, String subject,
            Map<String, MappedProperty> properties) {
        if (siteKey != null && siteKey.trim().isEmpty()) {
            throw new IllegalArgumentException("A blank site key names no site; pass null when the result states none");
        }
        this.loginId = loginId;
        this.siteKey = siteKey;
        this.connectorName = connectorName;
        this.subject = subject;
        this.properties = properties == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public String getLoginId() {
        return loginId;
    }

    public String getSiteKey() {
        return siteKey;
    }

    /**
     * @return the connector whose identity provider asserted {@link #getSubject()}, or {@code null}
     *         when this result states no asserted identity
     */
    public String getConnectorName() {
        return connectorName;
    }

    /**
     * @return the subject the identity provider asserted, or {@code null} when this result states
     *         none. This is what resolves the account, and {@link #getLoginId()} is what names it.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @return the profile properties, in insertion order, without the login id and the site key
     */
    public Map<String, MappedProperty> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return loginId + "@" + siteKey;
    }
}
