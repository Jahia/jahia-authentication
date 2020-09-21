package org.jahia.modules.jahiaauth.service;

import java.io.IOException;

public interface SettingsService {
    Settings getSettings(String site);

    void storeSettings(Settings settings) throws IOException;

    ConnectorConfig getConnectorConfig(String siteKey, String connectorName);
}
