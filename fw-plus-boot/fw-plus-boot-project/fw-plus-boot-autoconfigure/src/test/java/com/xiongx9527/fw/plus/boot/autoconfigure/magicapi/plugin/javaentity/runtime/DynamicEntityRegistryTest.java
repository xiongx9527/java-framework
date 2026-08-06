package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicEntityRegistryTest {

    @Test
    void atomicallyPublishesNewGenerationAndRollsBackFailedUpdate() throws Exception {
        DynamicEntityRegistry registry = registry();
        JavaEntityInfo first = entity("user", "User",
                "package demo; public class User { public String first; }");

        assertTrue(registry.rebuild(Collections.singletonList(first)));
        DynamicEntityDescriptor firstDescriptor = registry.resolve("User");
        Class<?> firstClass = firstDescriptor.javaClass();

        JavaEntityInfo second = entity("user", "User",
                "package demo; public class User { public String second; }");
        assertTrue(registry.rebuild(Collections.singletonList(second)));
        DynamicEntityDescriptor secondDescriptor = registry.resolve("demo.User");

        assertEquals(1L, firstDescriptor.version());
        assertEquals(2L, secondDescriptor.version());
        assertNotSame(firstClass, secondDescriptor.javaClass());
        assertEquals("second", secondDescriptor.javaClass().getField("second").getName());

        JavaEntityInfo broken = entity("user", "User",
                "package demo; public class User { public int value = \"wrong\"; }");
        assertFalse(registry.rebuild(Collections.singletonList(broken)));
        assertSame(secondDescriptor.javaClass(), registry.resolve("User").javaClass());
        assertEquals(2L, registry.currentVersion());
        assertTrue(registry.status().isAvailable());
        assertFalse(registry.status().getDiagnostics().isEmpty());
    }

    @Test
    void reportsUnavailableWhenTheFirstCandidateFails() {
        DynamicEntityRegistry registry = registry();

        assertFalse(registry.rebuild(Collections.singletonList(entity("user", "User",
                "package demo; public class User { public int value = \"wrong\"; }"))));

        assertFalse(registry.status().isAvailable());
        assertEquals(0L, registry.status().getVersion());
        assertFalse(registry.status().getDiagnostics().isEmpty());
        IllegalStateException unavailable = assertThrows(IllegalStateException.class,
                () -> registry.resolve("User"));
        assertTrue(unavailable.getMessage().contains("动态实体不可用"));
        assertTrue(unavailable.getMessage().contains(registry.status().getDiagnostics().get(0).getMessage()));
    }

    @Test
    void publishesConflictingSimpleNamesWithoutRegisteringAnAmbiguousAlias() {
        DynamicEntityRegistry registry = registry();
        assertTrue(registry.rebuild(Arrays.asList(
                entity("one", "User", "package one; public class User {}"),
                entity("two", "User", "package two; public class User {}"))));

        assertEquals("one.User", registry.resolve("one.User").name());
        assertEquals("two.User", registry.resolve("two.User").name());
        assertThrows(IllegalArgumentException.class, () -> registry.resolve("User"));
    }

    @Test
    void recordsResourceIdentityAndSourceDigestInPublishedDescriptor() {
        DynamicEntityRegistry registry = registry();
        assertTrue(registry.rebuild(Collections.singletonList(entity("resource-one", "User",
                "package one; public class User {}"))));

        DynamicEntityDescriptor descriptor = registry.resolve("one.User");

        assertEquals("resource-one", descriptor.resourceId());
        assertEquals("9f516b7dbec55a3789cebf9bda5137d60db639b77aa115e13e74f540257c7a66",
                descriptor.sourceDigest());
    }

    @Test
    void rejectsDuplicateFullyQualifiedClassNameWithoutChangingCurrentGeneration() {
        DynamicEntityRegistry registry = registry();
        assertTrue(registry.rebuild(Collections.singletonList(entity("first", "User",
                "package demo; public class User {}"))));

        assertFalse(registry.rebuild(Arrays.asList(
                entity("first", "User", "package demo; public class User {}"),
                entity("second", "User", "package demo; public class User { public String name; }"))));

        assertEquals(1L, registry.currentVersion());
        assertEquals("entity.class.duplicate", registry.status().getDiagnostics().get(0).getCode());
        assertEquals("second", registry.status().getDiagnostics().get(0).getResourceId());
    }

    @Test
    void publishesEmptyGenerationWhenLastEntityIsRemoved() {
        DynamicEntityRegistry registry = registry();
        assertTrue(registry.replace(entity("user", "User", "package demo; public class User {}")));

        assertTrue(registry.remove("user"));

        assertEquals(2L, registry.currentVersion());
        assertThrows(IllegalArgumentException.class, () -> registry.resolve("User"));
    }

    @Test
    void concurrentReadersNeverObserveMixedGenerationDescriptor() throws Exception {
        DynamicEntityRegistry registry = registry();
        assertTrue(registry.rebuild(Collections.singletonList(entity("user", "User",
                sourceWithGeneration(1L)))));
        ExecutorService readers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);
        Callable<Void> readTask = () -> {
            start.await();
            while (running.get()) {
                DynamicEntityDescriptor descriptor = registry.resolve("User");
                long classGeneration = ((Number) descriptor.javaClass()
                        .getMethod("generation").invoke(null)).longValue();
                assertEquals(descriptor.version(), classGeneration);
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(2L));
            }
            return null;
        };
        List<Future<Void>> results = Arrays.asList(readers.submit(readTask), readers.submit(readTask));

        try {
            start.countDown();
            assertTrue(registry.rebuild(Collections.singletonList(entity("user", "User",
                    sourceWithGeneration(2L)))));
        } finally {
            running.set(false);
            readers.shutdown();
        }
        for (Future<Void> result : results) {
            result.get(10, TimeUnit.SECONDS);
        }
        assertTrue(readers.awaitTermination(10, TimeUnit.SECONDS));
    }

    private static DynamicEntityRegistry registry() {
        return new DynamicEntityRegistry(new JavaSourceAnalyzer(), new InMemoryJavaEntityCompiler());
    }

    private static JavaEntityInfo entity(String id, String name, String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return info;
    }

    private static String sourceWithGeneration(long generation) {
        return "package demo; public class User { public static long generation() { return "
                + generation + "L; } }";
    }
}
