package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model;

import org.ssssssss.magicapi.core.model.PathMagicEntity;

public class JavaEntityInfo extends PathMagicEntity {

    @Override
    public JavaEntityInfo simple() {
        JavaEntityInfo info = new JavaEntityInfo();
        super.simple(info);
        return info;
    }

    @Override
    public JavaEntityInfo copy() {
        JavaEntityInfo info = new JavaEntityInfo();
        super.copyTo(info);
        return info;
    }
}
