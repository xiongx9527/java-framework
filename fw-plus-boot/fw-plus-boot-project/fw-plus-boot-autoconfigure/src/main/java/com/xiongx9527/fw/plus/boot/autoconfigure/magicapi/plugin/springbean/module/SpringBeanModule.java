package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime.DynamicSpringBeanRegistry;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@MagicModule("springBean")
public class SpringBeanModule implements DynamicAttribute<Object, SpringBeanModule>,
        DynamicModule<SpringBeanModule> {

    private final DynamicSpringBeanRegistry registry;

    public SpringBeanModule(DynamicSpringBeanRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Object getDynamicAttribute(String key) {
        return registry.getBean(key);
    }

    @Override
    public SpringBeanModule getDynamicModule(MagicScriptContext context) {
        return this;
    }
}
