package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.model;

import org.ssssssss.magicapi.core.model.PathMagicEntity;

public class SpringBeanInfo extends PathMagicEntity {

    private Boolean enabled = Boolean.TRUE;
    private String description;

    public Boolean getEnabled() {
        return enabled;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public SpringBeanInfo simple() {
        SpringBeanInfo info = new SpringBeanInfo();
        super.simple(info);
        info.setEnabled(getEnabled());
        info.setDescription(getDescription());
        return info;
    }

    @Override
    public SpringBeanInfo copy() {
        SpringBeanInfo info = new SpringBeanInfo();
        super.copyTo(info);
        info.setEnabled(getEnabled());
        info.setDescription(getDescription());
        return info;
    }
}
