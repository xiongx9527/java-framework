package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityDescriptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@MagicModule("entity")
public class EntityModule implements DynamicAttribute<DynamicEntityDescriptor, EntityModule>,
        DynamicModule<EntityModule> {

    private final DynamicEntityRegistry registry;

    public EntityModule(DynamicEntityRegistry registry) {
        this.registry = registry;
    }

    @Override
    public DynamicEntityDescriptor getDynamicAttribute(String key) {
        return registry.resolve(key);
    }

    @Override
    public EntityModule getDynamicModule(MagicScriptContext context) {
        return this;
    }
}
