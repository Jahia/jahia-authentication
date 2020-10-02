package org.jahia.modules.jahiaauth.service;

import java.io.Serializable;

public class MappedProperty implements Serializable {

    private MappedPropertyInfo info;
    private Serializable value;

    public MappedPropertyInfo getInfo() {
        return info;
    }

    public void setInfo(MappedPropertyInfo info) {
        this.info = info;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = (Serializable) value;
    }

    public MappedProperty(MappedPropertyInfo info, Object value) {
        this.info = info;
        this.value = (Serializable) value;
    }
}
