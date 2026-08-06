package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicEntityDescriptorTest {

    @Test
    void createMapUsesSetterThenWritableFieldFallbackAndConvertsValues() throws Exception {
        DynamicEntityDescriptor descriptor = descriptor("User",
                "package demo; public class User {\n"
                        + "  private String name;\n"
                        + "  private int age;\n"
                        + "  private Status status;\n"
                        + "  public boolean setterCalled;\n"
                        + "  public enum Status { ACTIVE }\n"
                        + "  public void setName(String value) { setterCalled = true; name = value; }\n"
                        + "  public String getName() { return name; }\n"
                        + "}\n");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "Alice");
        values.put("age", Long.valueOf(18L));
        values.put("status", "ACTIVE");

        Object user = descriptor.create(values);

        assertEquals("Alice", user.getClass().getMethod("getName").invoke(user));
        assertTrue(user.getClass().getField("setterCalled").getBoolean(user));
        assertEquals(18, field(user, "age"));
        assertEquals("ACTIVE", String.valueOf(field(user, "status")));
    }

    @Test
    void createRejectsUnknownPropertyAndMissingNoArgConstructor() {
        DynamicEntityDescriptor descriptor = descriptor("User", "public class User {}");
        Map<String, Object> values = Collections.<String, Object>singletonMap("missing", "value");

        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> descriptor.create(values));
        assertTrue(unknown.getMessage().contains("User"));
        assertTrue(unknown.getMessage().contains("missing"));

        DynamicEntityDescriptor constructorOnly = descriptor("ConstructorOnly",
                "public class ConstructorOnly { public ConstructorOnly(String value) {} }");
        IllegalStateException noConstructor = assertThrows(IllegalStateException.class, constructorOnly::create);
        assertTrue(noConstructor.getMessage().contains("ConstructorOnly"));
    }

    @Test
    void createRejectsStaticSetterAndInconvertibleValue() {
        DynamicEntityDescriptor descriptor = descriptor("User",
                "public class User {\n"
                        + "  private static String global;\n"
                        + "  private int age;\n"
                        + "  public static void setGlobal(String value) { global = value; }\n"
                        + "}\n");

        IllegalArgumentException staticProperty = assertThrows(IllegalArgumentException.class,
                () -> descriptor.create(Collections.<String, Object>singletonMap("global", "value")));
        assertTrue(staticProperty.getMessage().contains("User"));
        assertTrue(staticProperty.getMessage().contains("global"));

        IllegalArgumentException wrongType = assertThrows(IllegalArgumentException.class,
                () -> descriptor.create(Collections.<String, Object>singletonMap("age", "not-a-number")));
        assertTrue(wrongType.getMessage().contains("User"));
        assertTrue(wrongType.getMessage().contains("age"));
    }

    @Test
    void propertyMetadataDoesNotRetainRetiredGenerationClassLoader() throws Exception {
        WeakReference<ClassLoader> classLoader = createAndReleaseGeneration();

        for (int attempt = 0; attempt < 40 && classLoader.get() != null; attempt++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(25L);
        }

        assertNull(classLoader.get(), "属性元数据不应通过全局缓存固定退休代际 ClassLoader");
    }

    private static WeakReference<ClassLoader> createAndReleaseGeneration() {
        DynamicEntityRegistry registry = new DynamicEntityRegistry(
                new JavaSourceAnalyzer(), new InMemoryJavaEntityCompiler());
        try {
            JavaEntityInfo info = new JavaEntityInfo();
            info.setId("User");
            info.setName("User");
            info.setScript("package retired; public class User {"
                    + " private String name;"
                    + " public void setName(String value) { this.name = value; }"
                    + " }");
            assertTrue(registry.rebuild(Collections.singletonList(info)));
            DynamicEntityDescriptor descriptor = registry.resolve("User");
            descriptor.create(Collections.<String, Object>singletonMap("name", "Alice"));
            return new WeakReference<>(descriptor.javaClass().getClassLoader());
        } finally {
            registry.close();
        }
    }

    private static Object field(Object target, String name) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static DynamicEntityDescriptor descriptor(String name, String source) {
        DynamicEntityRegistry registry = new DynamicEntityRegistry(
                new JavaSourceAnalyzer(), new InMemoryJavaEntityCompiler());
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(name);
        info.setName(name);
        info.setScript(source);
        assertTrue(registry.rebuild(Collections.singletonList(info)));
        return registry.resolve(name);
    }
}
