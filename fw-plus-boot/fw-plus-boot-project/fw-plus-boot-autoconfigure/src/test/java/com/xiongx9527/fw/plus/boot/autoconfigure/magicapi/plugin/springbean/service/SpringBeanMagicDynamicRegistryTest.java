package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.model.SpringBeanInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime.DynamicSpringBeanRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringBeanMagicDynamicRegistryTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void disabledSpringBeanResourcesStayInSnapshotButDoNotRegisterUntilEnabled() {
        context = new AnnotationConfigApplicationContext();
        RecordingSpringBeanRegistry springBeanRegistry = new RecordingSpringBeanRegistry(context);
        SpringBeanMagicResourceStorage storage =
                new SpringBeanMagicResourceStorage(new JavaSourceAnalyzer(), springBeanRegistry);
        SpringBeanMagicDynamicRegistry registry =
                new SpringBeanMagicDynamicRegistry(storage, springBeanRegistry);
        SpringBeanInfo service = bean("svc", "DemoService", "package demo; public class DemoService {}");
        service.setEnabled(false);

        assertTrue(registry.register(service));
        assertTrue(springBeanRegistry.lastSnapshot.isEmpty());
        assertNull(registry.getMapping("demo.DemoService"));

        service.setEnabled(true);
        assertTrue(registry.register(service));
        assertEquals(1, springBeanRegistry.lastSnapshot.size());
        assertEquals("svc", registry.getMapping("demo.DemoService").getId());
    }

    @Test
    void copyPreservesEnabledFlagAndDescription() {
        SpringBeanInfo disabled = bean("svc", "DemoService", "package demo; public class DemoService {}");
        disabled.setEnabled(false);
        disabled.setDescription("demo service");

        assertFalse(disabled.copy().isEnabled());
        assertFalse(disabled.simple().isEnabled());
        assertEquals("demo service", disabled.copy().getDescription());
        assertEquals("demo service", disabled.simple().getDescription());
    }

    private static SpringBeanInfo bean(String id, String name, String source) {
        SpringBeanInfo info = new SpringBeanInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return info;
    }

    private static final class RecordingSpringBeanRegistry extends DynamicSpringBeanRegistry {

        private List<SpringBeanInfo> lastSnapshot = new ArrayList<>();

        private RecordingSpringBeanRegistry(AnnotationConfigApplicationContext context) {
            super(context, new JavaSourceAnalyzer(),
                    new InMemoryJavaEntityCompiler(null, SpringBeanMagicDynamicRegistryTest.class.getClassLoader()));
        }

        @Override
        public boolean rebuild(Collection<SpringBeanInfo> sources) {
            lastSnapshot = new ArrayList<>();
            for (SpringBeanInfo source : sources) {
                lastSnapshot.add(source.copy());
            }
            return true;
        }
    }
}
