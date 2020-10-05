package org.jahia.modules.jahiaauth.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MapperConfig {

    private String mapperName;
    private String siteKey;
    private boolean active;
    private List<Mapping> mappings = new ArrayList<>();
    private Settings.Values values;

    public MapperConfig(String mapperName, Settings.Values values) {
        this.mapperName = mapperName;
        this.values = values;
    }

    public String getMapperName() {
        return mapperName;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    public String getProperty(String name) {
        return values.getProperty(name);
    }

    public Boolean getBooleanProperty(String name) {
        return values.getBooleanProperty(name);
    }

    public byte[] getBinaryProperty(String name) {
        if (getProperty(name) == null) {
            return null;
        }
        return Base64.getDecoder().decode(getProperty(name));
    }

    public List<String> getListProperty(String name) {
        return values.getListProperty(name);
    }

}
