package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaEntityMagicResourceStorageTest {

    private final DynamicEntityRegistry registry = new DynamicEntityRegistry(
            new JavaSourceAnalyzer(), new InMemoryJavaEntityCompiler());
    private final JavaEntityMagicResourceStorage storage =
            new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer(), registry);

    @AfterEach
    void closeRegistry() {
        registry.close();
    }

    @Test
    void roundTripsCompleteUtf8JavaSourceAndBuildsFqcnMapping() {
        String source = "package demo;\npublic class User {\n"
                + "  public String label = \"中文\";\n}\n";
        JavaEntityInfo original = entity("user-id", "User", source);

        JavaEntityInfo restored = storage.read(storage.write(original));

        assertEquals("java-entity", storage.folder());
        assertEquals(".java", storage.suffix());
        assertEquals(JavaEntityInfo.class, storage.magicClass());
        assertFalse(storage.requirePath());
        assertTrue(storage.requiredScript());
        assertTrue(storage.allowRoot());
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getName(), restored.getName());
        assertEquals(source, restored.getScript());
        assertEquals("demo.User", storage.buildMappingKey(restored));
    }

    @Test
    void validateRejectsEmptySourceAndResourceNameMismatch() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.validate(entity("empty", "User", "")));
        assertThrows(IllegalArgumentException.class,
                () -> storage.validate(entity("mismatch", "Account", "public class User {}")));
    }

    @Test
    void validateCompilesTheCompletePersistedCandidateBeforeSave() {
        DynamicEntityRegistry registry = new DynamicEntityRegistry(
                new JavaSourceAnalyzer(), new InMemoryJavaEntityCompiler());
        JavaEntityMagicResourceStorage compilingStorage =
                new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer(), registry);
        MagicResourceService resourceService = mock(MagicResourceService.class);
        JavaEntityInfo dept = entity("dept", "Dept", "package demo; public class Dept {}");
        when(resourceService.<JavaEntityInfo>files("java-entity"))
                .thenReturn(Collections.singletonList(dept));
        compilingStorage.setMagicResourceService(resourceService);
        try {
            JavaEntityInfo valid = entity("user", "User",
                    "package demo; public class User { private Dept dept; }");
            assertDoesNotThrow(() -> compilingStorage.validate(valid));

            JavaEntityInfo broken = entity("user", "User",
                    "package demo; public class User { private MissingType value; }");
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> compilingStorage.validate(broken));
            assertTrue(exception.getMessage().contains("MissingType"));
            assertEquals(0L, registry.currentVersion());
        } finally {
            registry.close();
        }
    }

    private static JavaEntityInfo entity(String id, String name, String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return info;
    }
}
