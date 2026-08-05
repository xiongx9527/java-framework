package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaEntityMagicResourceStorageTest {

    private final JavaEntityMagicResourceStorage storage =
            new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer());

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

    private static JavaEntityInfo entity(String id, String name, String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return info;
    }
}
