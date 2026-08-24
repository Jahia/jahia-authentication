/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jahiaauth.service;

/**
 * Constants use across the application
 *
 * @author dgaillard
 */
public class JahiaAuthConstants {
    public static final String JAHIA_AUTH_USER_CACHE = "JahiaAuthUserCache";
    public static final String SSO_LOGIN = "ssoLoginId";
    /**
     * The subject the identity provider asserted, as the connector declared it.
     * <p>
     * It travels through a mapper result beside the login id, and it is not a mapping: the
     * connector names the property carrying it, and a configuration cannot name another one. The
     * login id is the name of the account and is freely chosen, so the subject is what resolves
     * the account.
     */
    public static final String SSO_SUBJECT = "ssoSubject";
    public static final String SITE_KEY = "siteKey";
    public static final String MAPPER_SERVICE_NAME = "mapperServiceName";
    public static final String MAPPERS_NODE_NAME = "mappers";

    /** The node type of one link between an account and the identity a provider asserted. */
    public static final String SSO_IDENTITY_LINK_TYPE = "authnt:ssoIdentityLink";
    /**
     * The node type holding the links of one account. It carries the access control list that grants
     * nobody, and it is the node an account is authorized to hold: the grant a module needs to place a
     * child under a user node names this type.
     */
    public static final String SSO_IDENTITY_LINKS_TYPE = "authnt:ssoIdentityLinks";
    /** The name of that node under the account. */
    public static final String SSO_IDENTITY_LINKS_NODE = "ssoIdentityLinks";
    public static final String PROPERTY_CONNECTOR_NAME = "connectorName";
    public static final String PROPERTY_SUBJECT = "subject";

    public static final String PROPERTY_IS_ENABLED = "enabled";

    public static final String PROPERTY_MAPPING = "mapping";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_MANDATORY = "mandatory";
    public static final String PROPERTY_SITE_KEY = "siteKey";
    public static final String PROPERTY_VALUE_FORMAT = "valueFormat";
    public static final String PROPERTY_VALUE_TYPE = "valueType";
    public static final String PROPERTY_VALUE = "value";

    public static final String CONNECTOR_NAME_AND_ID = "connectorNameAndID";
    public static final String PROPERTIES = "properties";
    public static final String METHOD_GET = "GET";
    public static final String CONNECTOR_SERVICE_NAME = "connectorServiceName";
    public static final String CONNECTOR = "connector";
    public static final String MAPPER = "mapper";

    private JahiaAuthConstants() {
    }
}
