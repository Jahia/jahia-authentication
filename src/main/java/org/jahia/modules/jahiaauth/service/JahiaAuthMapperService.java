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

import java.util.Map;

/**
 * Service to be implemented by Mapper that need to access the data in the cache
 *
 * @author dgaillard
 */
public interface JahiaAuthMapperService {
    /**
     * This method will register the results for the mapper in the cache during 180 seconds
     * <p>
     * A cached result is expected to carry, under {@link JahiaAuthConstants#SITE_KEY}, the site key the
     * connector that produced it is configured on: that is the site an account is resolved against when
     * the entry is later used to log a user in. {@link #executeMapper} records it for you. An entry cached
     * through this method without it resolves accounts at server level only, so a connector seeding the
     * cache directly should set it from its own {@link MapperConfig#getSiteKey()}.
     *
     * @param mapperServiceName the mapper service name, which the cache key is built from together with the session ID
     * @param sessionId the user session ID
     * @param mapperResult map that contains the result for the mapper
     */
    void cacheMapperResults(String mapperServiceName, String sessionId, Map<String, MappedProperty> mapperResult);

    Map<String, MappedProperty> getCachedMapperResults(String mapperServiceName, String sessionId);

    /**
     * Runs the mapper for a connector's results and caches them, recording the connector's configured
     * site key on the cached entry — the preferred entry point over {@link #cacheMapperResults}.
     *
     * @param sessionId the user session ID
     * @param mapperConfig the mapper configuration, which carries the connector's site key
     * @param connectorProperties the properties the connector obtained from the identity provider
     * @throws JahiaAuthException if a mandatory mapped property is missing
     */
    void executeMapper(String sessionId, MapperConfig mapperConfig, Map<String, Object> connectorProperties) throws JahiaAuthException;

    /**
     * This method will execute all the implementation of {@link ConnectorResultProcessor} available in the current OSGI context
     * This allows for custom code execution right after a successful authentication, example use case:
     * - retrieve data from the TOKEN and store this data in the authenticated user
     * - extract additionals info from the results given by the auth provider.
     * @param connectorConfig the connector Config that was used to establish the connection
     * @param results the results of the authentication including tokens data.
     */
    void executeConnectorResultProcessors(ConnectorConfig connectorConfig, Map<String, Object> results);

    /**
     * This method get the results of the mapper in the cache
     * @param cacheKey String the cache key built using the user session ID and the mapper service name
     * @return HashMap that contains the result for the mapper
     */
    Map<String, Map<String, MappedProperty>> getMapperResultsForSession(String sessionId);

    /**
     * This method will update all the cache entries that contains the original session ID with the new session ID
     * This method should be use is the session has been invalidated to ensure that other mappers will be able to find the results with the new session ID
     * @param originalSessionId String of the original session ID
     * @param newSessionId String of the new session ID
     */
    void updateCacheEntry(String originalSessionId, String newSessionId);
}
