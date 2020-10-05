package org.jahia.modules.jahiaauth.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class ConnectorConfig {

    private Settings settings;
    private Settings.Values values;
    private List<MapperConfig> mappers = new ArrayList<>();

    public ConnectorConfig(Settings settings, String connectorName) {
        this.settings = settings;
        this.values = settings.getValues(connectorName);

        try {
            initMappers(settings);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initMappers(Settings settings) throws JSONException {
        Settings.Values mappersValues = values.getSubValues(JahiaAuthConstants.MAPPERS_NODE_NAME);
        for (String subValueKey : mappersValues.getSubValueKeys()) {
            Settings.Values mapper = mappersValues.getSubValues(subValueKey);

            JSONArray mappingsJson = new JSONArray(mapper.getProperty(JahiaAuthConstants.PROPERTY_MAPPING));

            Map<String, Mapping> mappingsMap = new HashMap<>();
            Mapping loginMapping = new Mapping();
            loginMapping.setConnectorProperty(JahiaAuthConstants.CONNECTOR_NAME_AND_ID);
            loginMapping.setMappedProperty(JahiaAuthConstants.SSO_LOGIN);
            mappingsMap.put(JahiaAuthConstants.SSO_LOGIN, loginMapping);

            Mapping siteMapping = new Mapping();
            siteMapping.setConnectorProperty(JahiaAuthConstants.PROPERTY_SITE_KEY);
            siteMapping.setMappedProperty(JahiaAuthConstants.SITE_KEY);
            mappingsMap.put(JahiaAuthConstants.SITE_KEY, siteMapping);

            for (int i = 0; i < mappingsJson.length(); i++) {
                JSONObject jsonObject = mappingsJson.getJSONObject(i);
                JSONObject mapperJson = jsonObject.getJSONObject(JahiaAuthConstants.MAPPER);
                JSONObject connectorJson = jsonObject.getJSONObject(JahiaAuthConstants.CONNECTOR);

                Mapping mapping = new Mapping();
                mapping.setConnectorProperty(connectorJson.getString(JahiaAuthConstants.PROPERTY_NAME));
                mapping.setMappedProperty(mapperJson.getString(JahiaAuthConstants.PROPERTY_NAME));
                mappingsMap.put(mapping.getMappedProperty(), mapping);
            }

            MapperConfig mapperConfig = new MapperConfig(subValueKey, mapper);
            mapperConfig.setSiteKey(settings.getSiteKey());
            mapperConfig.setActive(mapper.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED));
            mapperConfig.getMappings().addAll(mappingsMap.values());
            this.mappers.add(mapperConfig);
        }
    }

    public String getSiteKey() {
        return settings.getSiteKey();
    }

    public String getConnectorName() {
        return values.getPath();
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

    public List<MapperConfig> getMappers() {
        return mappers;
    }

    public MapperConfig getMapper(String name) {
        for (MapperConfig mapper : mappers) {
            if (mapper.getMapperName().equals(name)) {
                return mapper;
            }
        }
        return null;
    }

    public Settings.Values getValues() {
        return values;
    }

}
