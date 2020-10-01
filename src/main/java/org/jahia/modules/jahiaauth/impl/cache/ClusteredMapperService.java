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
package org.jahia.modules.jahiaauth.impl.cache;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.utils.ClassLoaderUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dgaillard
 */
public class ClusteredMapperService implements CacheService {
    private HazelcastInstance hazelcastInstance;

    public ClusteredMapperService(Object hazelcast) {
        this.hazelcastInstance = (HazelcastInstance) hazelcast;
        Config config = hazelcastInstance.getConfig();
        MapConfig mapConfig = new MapConfig(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).setTimeToLiveSeconds(180);
        config.addMapConfig(mapConfig);
    }

    @Override
    public void cacheMapperResults(String cacheKey, Map<String, MappedProperty> mapperResult) {
        if (hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).containsKey(cacheKey)) {
            hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).replace(cacheKey, mapperResult);
        } else {
            hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).set(cacheKey, mapperResult);
        }
    }

    @Override
    public Map<String, MappedProperty> getCachedMapperResults(String cacheKey) {
        Map<String, MappedProperty> mapperResult = null;
        if (hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).containsKey(cacheKey)) {
            mapperResult = (Map<String, MappedProperty>) hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).get(cacheKey);
        }
        return mapperResult;
    }

    @Override
    public Map<String, Map<String, MappedProperty>> getMapperResultsForSession(String sessionId) {
        Map<String, Map<String, MappedProperty>> res = new HashMap<>();
        for (Object key : hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).keySet()) {
            String keyAsString = (String) key;
            if (StringUtils.endsWith(keyAsString, sessionId)) {
                String mapper = StringUtils.substringBefore(keyAsString, "_" + sessionId);
                Map<String, MappedProperty> mapperResult = ClassLoaderUtils.executeWith(ClusteredMapperService.class.getClassLoader(), () ->
                    (Map<String, MappedProperty>) hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).get(key)
                );
                res.put(mapper, mapperResult);
            }
        }
        return res;
    }

    @Override
    public void updateCacheEntry(String originalSessionId, String newSessionId) {
        for (Object key : hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).keySet()) {
            String keyAsString = (String) key;
            if (StringUtils.endsWith(keyAsString, originalSessionId)) {
                String newKey = StringUtils.substringBefore(keyAsString, originalSessionId) + newSessionId;
                ClassLoaderUtils.executeWith(ClusteredMapperService.class.getClassLoader(), () -> {
                    Map<String, MappedProperty> mapperResult = (Map<String, MappedProperty>) hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).get(key);
                    hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).remove(key);
                    hazelcastInstance.getMap(JahiaAuthConstants.JAHIA_AUTH_USER_CACHE).set(newKey, mapperResult);
                    return null;
                });
            }
        }
    }
}
