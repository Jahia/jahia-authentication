package org.jahia.modules.jahiaauth.impl.cache;

import org.jahia.modules.jahiaauth.service.MappedProperty;

import java.util.Map;

public interface CacheService {
    void cacheMapperResults(String cacheKey, Map<String, MappedProperty> mapperResult);

    Map<String, MappedProperty> getCachedMapperResults(String cacheKey);

    Map<String, Map<String, MappedProperty>> getMapperResultsForSession(String sessionId);

    void updateCacheEntry(String originalSessionId, String newSessionId);

}
