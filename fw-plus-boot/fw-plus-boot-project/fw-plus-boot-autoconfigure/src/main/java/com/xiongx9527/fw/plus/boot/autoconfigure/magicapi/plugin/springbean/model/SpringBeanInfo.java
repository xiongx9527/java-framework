package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;

public class SpringBeanInfo extends JavaEntityInfo {

    @Override
    public SpringBeanInfo simple() {
        SpringBeanInfo info = new SpringBeanInfo();
        super.simple(info);
        info.setEnabled(getEnabled());
        return info;
    }

    @Override
    public SpringBeanInfo copy() {
        SpringBeanInfo info = new SpringBeanInfo();
        return super.copyTo(info);
    }
}
