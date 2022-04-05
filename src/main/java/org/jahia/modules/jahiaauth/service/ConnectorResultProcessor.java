package org.jahia.modules.jahiaauth.service;

import java.util.Map;

/**
 * This interface is completely different from the other Mapper classes available.
 *
 * It's used directly as a Service without any UIs to configure it.
 * It will only be triggered after getting the result of a user's connection
 * It will receive all the properties available from the connection including the token data.
 *
 * This allows for more programmatically approach and more control to perform additional operations right after a connector established a connection.
 */
public interface ConnectorResultProcessor {
    /**
     * The implementation of the ConnectorResultProcessor execution.
     */
    void execute(ConnectorConfig connectorConfig, Map<String, Object> results);
}
