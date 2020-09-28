package org.jahia.modules.jahiaauth.impl;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.Settings;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SettingsServiceImpl implements SettingsService, ManagedServiceFactory {
    private static final SettingsServiceImpl INSTANCE = new SettingsServiceImpl();
    private Map<String, Settings> settingsBySiteKeyMap = new HashMap<>();
    private Map<String, Settings> settingsByPid = new HashMap<>();
    private ConfigurationAdmin configurationAdmin;

    private SettingsServiceImpl() {
        super();
    }

    public static SettingsServiceImpl getInstance() {
        return INSTANCE;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }


    public void registerServerSettings(Settings settings) {
        settingsBySiteKeyMap.put(settings.getSiteKey(), settings);
    }

    public void removeServerSettings(String siteKey) {
        if (settingsBySiteKeyMap.containsKey(siteKey)) {
            settingsBySiteKeyMap.remove(siteKey);
        }
    }

    public Settings getSettings(String site) {
        if (settingsBySiteKeyMap.containsKey(site)) {
            return settingsBySiteKeyMap.get(site);
        }
        Settings settings = new Settings();
        settings.setSiteKey(site);
        return settings;
    }

    public ConnectorConfig getConnectorConfig(String siteKey, String connectorName) {
        Settings settings = getSettings(siteKey);
        if (!settings.getValues(connectorName).isEmpty()) {
            return new ConnectorConfig(settings, connectorName);
        }
        return null;
    }

    public void storeSettings(Settings settings) throws IOException {
        // refresh and save settings
        Configuration configuration = findConfiguration(settings.getSiteKey());

        if (configuration.getProperties() == null) {
            @SuppressWarnings("java:S1149") Dictionary<String, Object> properties = new Hashtable<>();
            File file = new File(SettingsBean.getInstance().getJahiaVarDiskPath(), "karaf/etc/org.jahia.modules.auth-" + settings.getSiteKey() + ".cfg");
            properties.put("felix.fileinstall.filename", file.toURI().toString());

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write("# Auth Configuration file - autogenerated");
                bw.newLine();
                setProperties(settings, properties, bw);
                configuration.update(properties);
            }
        } else {
            Dictionary<String, Object> properties = configuration.getProperties();
            setProperties(settings, properties, null);
            configuration.update(properties);
        }
    }

    @Override
    public String getName() {
        return "Jahia Authentication Settings Service";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        if (settingsByPid.get(pid) == null) {
            Settings setting = new Settings();
            settingsByPid.put(pid, setting);
            setting.setSettingsService(this);
            setting.update(getMap(properties));
        } else {
            settingsByPid.get(pid).update(getMap(properties));
        }
    }

    private Map<String, Object> getMap(Dictionary<String, ?> d) {
        Map<String,Object> m = new HashMap<>();
        Enumeration<String> en = d.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            m.put(key, d.get(key));
        }
        return m;
    }


    @Override
    public void deleted(String pid) {
        if (settingsByPid.get(pid) != null) {
            settingsByPid.get(pid).destroy();
            settingsByPid.remove(pid);
        }
    }

    private void setProperties(Settings settings, Dictionary<String, Object> properties, BufferedWriter writer) throws IOException {
        Map<String, String> p = new TreeMap<>(settings.getProperties());
        setProperty(properties, writer, "siteKey", p.remove("siteKey"));
        for (Map.Entry<String, String> entry : p.entrySet()) {
            setProperty(properties, writer, entry.getKey(), entry.getValue());
        }
    }

    private void setProperty(Dictionary<String, Object> properties, BufferedWriter writer, String key, String value) throws IOException {
        if (value != null) {
            properties.put(key, value);
            if (writer != null) {
                writer.write(key + " = " + value);
                writer.newLine();
            }
        }
    }

    private Configuration findConfiguration(String siteKey) throws IOException {
        try {
            Configuration[] configurations = configurationAdmin.listConfigurations("(service.factoryPid=org.jahia.modules.auth)");
            for (Configuration configuration : configurations) {
                if (siteKey.equals(configuration.getProperties().get("siteKey"))) {
                    return configuration;
                }
            }
        } catch (InvalidSyntaxException e) {
            // not possible
        }
        return configurationAdmin.createFactoryConfiguration("org.jahia.modules.auth");
    }

}
