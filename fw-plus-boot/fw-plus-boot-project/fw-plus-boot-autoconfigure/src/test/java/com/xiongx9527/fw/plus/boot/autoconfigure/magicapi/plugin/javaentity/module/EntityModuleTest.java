package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityDescriptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import org.junit.jupiter.api.Test;
import org.ssssssss.script.MagicScriptContext;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityModuleTest {

    @Test
    void dynamicAttributeAlwaysResolvesDescriptorFromCurrentGeneration() {
        DynamicEntityRegistry registry = new DynamicEntityRegistry(
                new JavaSourceAnalyzer(), new InMemoryJavaEntityCompiler());
        assertTrue(registry.rebuild(Collections.singletonList(entity(
                "package demo; public class User { public String first; }"))));
        EntityModule module = new EntityModule(registry);

        DynamicEntityDescriptor first = module.getDynamicAttribute("User");
        assertTrue(registry.rebuild(Collections.singletonList(entity(
                "package demo; public class User { public String second; }"))));
        DynamicEntityDescriptor second = module.getDynamicAttribute("User");

        assertSame(module, module.getDynamicModule(new MagicScriptContext()));
        assertEquals(1L, first.version());
        assertEquals(2L, second.version());
        assertEquals("demo.User", second.name());
        assertSame(first.javaClass(), first.create().getClass());
    }

    private static JavaEntityInfo entity(String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId("user");
        info.setName("User");
        info.setScript(source);
        return info;
    }
}
