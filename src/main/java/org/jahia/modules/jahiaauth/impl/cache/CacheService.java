package org.jahia.modules.jahiaauth.impl.cache;

import org.jahia.modules.jahiaauth.service.MappedProperty;

import java.util.Map;

public interface CacheService {
    void cacheMapperResults(String cacheKey, Map<String, MappedProperty> mapperResult);

    Map<String, MappedProperty> getCachedMapperResults(String cacheKey);

    Map<String, Map<String, MappedProperty>> getMapperResultsForSession(String sessionId);

    void updateCacheEntry(String originalSessionId, String newSessionId);

    /**
     * Store a short-lived opaque String value under an exact key, in a cache separate from the mapper
     * results. Cluster-wide when clustering is active. Intended for transient auth-flow values (e.g. an
     * OAuth state token) that must survive across nodes without relying on HTTP session replication.
     */
    void cacheValue(String cacheKey, String value);

    /** Return the value stored via {@link #cacheValue(String, String)} for an exact key, or {@code null}. */
    String getCachedValue(String cacheKey);

    /** Remove a value stored via {@link #cacheValue(String, String)} (single-use consumption). */
    void invalidate(String cacheKey);

}
