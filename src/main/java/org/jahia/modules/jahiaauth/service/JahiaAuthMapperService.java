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

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Runs the mappers of a connector, and holds what they resolved for the session being authenticated.
 * <p>
 * The results live on the HTTP session that the identity provider called back. A cluster shares them
 * through the distributed sessions module, so one mechanism serves a single node and a cluster alike.
 * Earlier versions held them in a cache keyed by a session id, with a separate implementation per
 * deployment shape.
 *
 * @author dgaillard
 */
public interface JahiaAuthMapperService {

    /**
     * Runs every mapper of a connector against the properties it obtained, then records what each
     * mapper resolved on the session.
     *
     * @param request the request the identity provider called back
     * @param mapperConfig the mapper configuration, which carries the site key of the connector
     * @param connectorProperties the properties the connector obtained from the identity provider
     * @throws JahiaAuthException if a mandatory mapped property is missing
     */
    void executeMapper(HttpServletRequest request, MapperConfig mapperConfig, Map<String, Object> connectorProperties) throws JahiaAuthException;

    /**
     * Records what a connector read from its provider, beside the results of the mappers.
     * <p>
     * The recorded result describes the person and names no account. An account is named by a
     * mapping, because a mapping states which property of the connector the name is read from, and
     * that statement is what the rule on the source of a login id is checked against. A result built
     * here carries no such statement, so it takes no part in resolving the account.
     * <p>
     * A connector that reaches an account name of its own has one way to publish it: declare the
     * property it carries through {@code ConnectorService.getVerifiedSubjectProperty}, and let a
     * mapping read the login id from that property.
     *
     * @param request the request the identity provider called back
     * @param connectorName the name the result is recorded under
     * @param properties what the connector read, keyed by property name
     */
    void recordConnectorProperties(HttpServletRequest request, String connectorName,
            Map<String, MappedProperty> properties);

    /**
     * @param request the request being served
     * @return every result the session holds, keyed by mapper name, empty when it holds none
     */
    Map<String, MapperResult> getMapperResults(HttpServletRequest request);

    /**
     * @param request the request being served
     * @param mapperName the mapper whose result is wanted
     * @return the result of that mapper, or {@code null} when the session holds none
     */
    MapperResult getMapperResult(HttpServletRequest request, String mapperName);

    /**
     * This method will execute all the implementation of {@link ConnectorResultProcessor} available in the current OSGI context
     * This allows for custom code execution right after a successful authentication, example use case:
     * - retrieve data from the TOKEN and store this data in the authenticated user
     * - extract additionals info from the results given by the auth provider.
     * @param httpRequest the request the identity provider called back
     * @param connectorConfig the connector Config that was used to establish the connection
     * @param results the results of the authentication including tokens data.
     */
    void executeConnectorResultProcessors(HttpServletRequest httpRequest, ConnectorConfig connectorConfig, Map<String, Object> results);
}
