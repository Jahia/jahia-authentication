package org.jahia.modules.jahiaauth.service;

public class Mapping {
    private String connectorProperty;
    private String mappedProperty;

    public Mapping() {
    }

    public Mapping(String connectorProperty, String mappedProperty) {
        this.connectorProperty = connectorProperty;
        this.mappedProperty = mappedProperty;
    }

    public String getConnectorProperty() {
        return connectorProperty;
    }

    public void setConnectorProperty(String connectorProperty) {
        this.connectorProperty = connectorProperty;
    }

    public String getMappedProperty() {
        return mappedProperty;
    }

    public void setMappedProperty(String mappedProperty) {
        this.mappedProperty = mappedProperty;
    }
}
