package org.jahia.modules.jahiaauth.service;

import java.util.ArrayList;
import java.util.List;

public class MapperConfig {

    private String mapperName;
    private String siteKey;
    private boolean active;
    private List<Mapping> mappings = new ArrayList<>();

    public MapperConfig(String mapperName) {
        this.mapperName = mapperName;
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
}
