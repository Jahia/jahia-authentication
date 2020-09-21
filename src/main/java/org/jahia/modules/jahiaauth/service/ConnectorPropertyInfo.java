package org.jahia.modules.jahiaauth.service;

public class ConnectorPropertyInfo {
    private String name;
    private String valueType;
    private String valueFormat;
    private String propertyToRequest;
    private String valuePath;

    public ConnectorPropertyInfo() {
    }

    public ConnectorPropertyInfo(String name, String valueType) {
        this.name = name;
        this.valueType = valueType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getValueFormat() {
        return valueFormat;
    }

    public void setValueFormat(String valueFormat) {
        this.valueFormat = valueFormat;
    }

    public String getPropertyToRequest() {
        return propertyToRequest;
    }

    public void setPropertyToRequest(String propertyToRequest) {
        this.propertyToRequest = propertyToRequest;
    }

    public String getValuePath() {
        return valuePath;
    }

    public void setValuePath(String valuePath) {
        this.valuePath = valuePath;
    }
}
