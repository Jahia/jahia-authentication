package org.jahia.modules.jahiaauth.service;

import java.io.Serializable;

public class MappedPropertyInfo implements Serializable {
    private String name;
    private boolean mandatory;
    private String valueType;
    private String format;

    public MappedPropertyInfo() {
    }

    public MappedPropertyInfo(String name) {
        this.name = name;
    }

    public MappedPropertyInfo(String name, String type, String format, boolean mandatory) {
        this.name = name;
        this.valueType = type;
        this.format = format;
        this.mandatory = mandatory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
