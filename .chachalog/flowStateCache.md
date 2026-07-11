---
jahia-authentication: minor
---

Add a small cluster-wide, short-lived key/value store to the mapper cache service (`cacheValue`/`getCachedValue`/`invalidate`, in a cache region separate from the mapper results). Lets authentication connectors keep transient flow values (e.g. an OAuth `state` token) that are retrievable on any cluster node without relying on HTTP session replication.
