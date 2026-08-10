package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.MagicEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaEntityMagicDynamicRegistryTest {

    @Test
    void retainsFailedCandidateSnapshotSoLaterResourcesCanCompleteTheGeneration() {
        RecordingEntityRegistry entityRegistry = new RecordingEntityRegistry();
        entityRegistry.minimumSources = 2;
        JavaEntityMagicResourceStorage storage =
                new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer(), entityRegistry);
        JavaEntityMagicDynamicRegistry registry =
                new JavaEntityMagicDynamicRegistry(storage, entityRegistry);

        assertFalse(registry.register(entity("user", "User", "package demo; public class User {}")));
        assertEquals(1, entityRegistry.lastSnapshot.size());

        assertTrue(registry.register(entity("dept", "Dept", "package demo; public class Dept {}")));
        assertEquals(2, entityRegistry.lastSnapshot.size());
        assertEquals("user", registry.getMapping("demo.User").getId());
        assertEquals("dept", registry.getMapping("demo.Dept").getId());
    }

    @Test
    void fileEventsRebuildTheCurrentFullSnapshotAndIgnoreOtherResourceTypes() {
        RecordingEntityRegistry entityRegistry = new RecordingEntityRegistry();
        JavaEntityMagicDynamicRegistry registry = new JavaEntityMagicDynamicRegistry(
                new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer(), entityRegistry), entityRegistry);
        JavaEntityInfo user = entity("user", "User", "package demo; public class User {}" );

        registry.onFileEvent(new FileEvent("other", EventAction.CREATE, user));
        assertEquals(0, entityRegistry.rebuildCount);

        registry.onFileEvent(new FileEvent("java-entity", EventAction.CREATE, user));
        assertEquals(1, entityRegistry.rebuildCount);
        assertEquals(1, entityRegistry.lastSnapshot.size());

        registry.onFileEvent(new FileEvent("java-entity", EventAction.DELETE, user));
        assertEquals(2, entityRegistry.rebuildCount);
        assertTrue(entityRegistry.lastSnapshot.isEmpty());
        assertNull(registry.getMapping("demo.User"));
    }

    @Test
    void invalidCandidateDoesNotBreakMappingLookupDuringColdStart() {
        RecordingEntityRegistry entityRegistry = new RecordingEntityRegistry();
        entityRegistry.minimumSources = 2;
        JavaEntityMagicDynamicRegistry registry = new JavaEntityMagicDynamicRegistry(
                new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer(), entityRegistry), entityRegistry);

        assertFalse(registry.register(entity("broken", "Broken", "public class")));

        assertNull(registry.getMapping("Broken"));
    }

    @Test
    void disabledEntityResourcesStayInSnapshotButDoNotPublishMappingsUntilEnabled() {
        RecordingEntityRegistry entityRegistry = new RecordingEntityRegistry();
        JavaEntityMagicDynamicRegistry registry = new JavaEntityMagicDynamicRegistry(
                new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer(), entityRegistry), entityRegistry);
        JavaEntityInfo user = entity("user", "User", "package demo; public class User {}");
        user.setEnabled(false);

        assertTrue(registry.register(user));
        assertTrue(entityRegistry.lastSnapshot.isEmpty());
        assertNull(registry.getMapping("demo.User"));

        user.setEnabled(true);
        assertTrue(registry.register(user));
        assertEquals(1, entityRegistry.lastSnapshot.size());
        assertEquals("user", registry.getMapping("demo.User").getId());
    }

    @Test
    void copyPreservesEnabledFlag() {
        JavaEntityInfo disabled = entity("user", "User", "package demo; public class User {}");
        disabled.setEnabled(false);

        assertFalse(disabled.copy().isEnabled());
        assertFalse(disabled.simple().isEnabled());
    }

    @Test
    void magicApiClearEventClearsTheWholeEntitySnapshot() {
        RecordingEntityRegistry entityRegistry = new RecordingEntityRegistry();
        JavaEntityMagicDynamicRegistry registry = new JavaEntityMagicDynamicRegistry(
                new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer(), entityRegistry), entityRegistry);
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("javaEntityMagicDynamicRegistry", registry);
        context.refresh();
        try {
            assertTrue(registry.register(entity("user", "User", "public class User {}")));

            context.publishEvent(new MagicEvent("clear", EventAction.CLEAR));

            assertEquals(2, entityRegistry.rebuildCount);
            assertTrue(entityRegistry.lastSnapshot.isEmpty());
            assertNull(registry.getMapping("User"));
        } finally {
            context.close();
        }
    }

    private static JavaEntityInfo entity(String id, String name, String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return info;
    }

    private static final class RecordingEntityRegistry extends DynamicEntityRegistry {

        private int minimumSources;
        private int rebuildCount;
        private List<JavaEntityInfo> lastSnapshot = new ArrayList<>();

        private RecordingEntityRegistry() {
            super(new JavaSourceAnalyzer(),
                    new InMemoryJavaEntityCompiler(null, JavaEntityMagicDynamicRegistryTest.class.getClassLoader()));
        }

        @Override
        public boolean rebuild(Collection<JavaEntityInfo> entities) {
            rebuildCount++;
            lastSnapshot = new ArrayList<>();
            for (JavaEntityInfo entity : entities) {
                lastSnapshot.add(entity.copy());
            }
            return lastSnapshot.size() >= minimumSources;
        }
    }
}
