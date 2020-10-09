package org.jahia.modules.jahiaauth.service;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaauth.impl.SettingsServiceImpl;

import java.util.*;
import java.util.stream.Collectors;

public class Settings {
    private SettingsServiceImpl settingsService;
    private String previousSiteKey;

    private Map<String, String> props = new HashMap<>();

    public SettingsServiceImpl getSettingsService() {
        return settingsService;
    }

    public void setSettingsService(SettingsServiceImpl settingsService) {
        this.settingsService = settingsService;
    }

    public void init() {
        settingsService.registerServerSettings(this);
        this.previousSiteKey = getSiteKey();
    }

    public void destroy() {
        settingsService.removeServerSettings(getSiteKey());
    }

    @SuppressWarnings("java:S1172")
    public void update(Map<String, Object> map) {
        if (previousSiteKey != null) {
            settingsService.removeServerSettings(previousSiteKey);
        }
        this.props = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!entry.getKey().startsWith("felix.") && !entry.getKey().startsWith("service.")) {
                this.props.put(entry.getKey(), entry.getValue().toString());
            }
        }
        init();
    }

    public String getSiteKey() {
        return props.get("siteKey");
    }

    public void setSiteKey(String siteKey) {
        this.props.put("siteKey", siteKey);
    }

    public Map<String, String> getProperties() {
        return props;
    }

    public Values getValues(String path) {
        return new Values(path);
    }

    private Map<String, String> evalPath(Map<String, String> settings, String path) {
        return path == null ? settings : settings.keySet().stream().filter(k -> k.startsWith(path + ".")).collect(Collectors.toMap(s -> s.substring(path.length() + 1), settings::get));
    }

    @Override
    public String toString() {
        return props.toString();
    }

    public class Values {
        private String path;
        private Map<String, String> currentProps;

        public Values(String path) {
            this.path = path;
            this.currentProps = evalPath(props, path);
        }

        public String getPath() {
            return path;
        }

        public String getProperty(String name) {
            return currentProps.get(name);
        }

        public Boolean getBooleanProperty(String name) {
            return Boolean.valueOf(currentProps.get(name));
        }

        public List<String> getListProperty(String name) {
            Map<String, String> m = evalPath(currentProps, name);
            List<String> l = new ArrayList<>();
            int i = 0;
            while (m.containsKey(String.valueOf(i))) {
                l.add(m.get(String.valueOf(i)));
                i++;
            }
            return l;
        }

        public void setProperty(String name, String value) {
            props.put(path + "." + name, value);
            this.currentProps = evalPath(props, path);
        }

        public void setProperty(String name, Boolean value) {
            props.put(path + "." + name, value.toString());
            this.currentProps = evalPath(props, path);
        }

        public void setListProperty(String name, List<String> values) {
            int i = 0;
            for (String value : values) {
                props.put(path + "." + name + "." + i, value);
                i++;
            }
            this.currentProps = evalPath(props, path);
        }

        public void setBinaryProperty(String name, byte[] data) {
            setProperty(name, Base64.getEncoder().encodeToString(data));
        }

        public Values getSubValues(String path) {
            String subPath = this.path == null ? path : (this.path + "." + path);
            return new Values(subPath);
        }

        public Set<String> getSubValueKeys() {
            Set<String> keys = new HashSet<>();
            for (String s : currentProps.keySet()) {
                int i = s.indexOf('.');
                if (i > 0 && !StringUtils.isNumeric(s.substring(i+1))) {
                    keys.add(s.substring(0, i));
                }
            }
            return keys;
        }

        public boolean isEmpty() {
            return currentProps.isEmpty();
        }
    }
}
