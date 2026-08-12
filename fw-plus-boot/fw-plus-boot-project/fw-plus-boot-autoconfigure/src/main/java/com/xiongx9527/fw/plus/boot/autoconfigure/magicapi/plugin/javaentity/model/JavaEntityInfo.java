package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model;

import org.ssssssss.magicapi.core.model.PathMagicEntity;

public class JavaEntityInfo extends PathMagicEntity {

    private Boolean enabled = Boolean.TRUE;

    public Boolean getEnabled() {
        return enabled;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public JavaEntityInfo simple() {
        JavaEntityInfo info = new JavaEntityInfo();
        super.simple(info);
        info.setEnabled(getEnabled());
        return info;
    }

    @Override
    public JavaEntityInfo copy() {
        JavaEntityInfo info = new JavaEntityInfo();
        super.copyTo(info);
        info.setEnabled(getEnabled());
        return info;
    }
}
