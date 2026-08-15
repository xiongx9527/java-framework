package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.CompiledGeneration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityCompilationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceUnit;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.module.EntityModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityDescriptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service.JavaEntityMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service.JavaEntityMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.starter.JavaEntityProperties;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicJavaEntityIntegrationTest {

    @Test
    void enforcesIndividualAndTotalUtf8SourceLimitsBeforeCompilation() {
        RecordingCompiler compiler = new RecordingCompiler();
        JavaEntityProperties properties = new JavaEntityProperties();
        properties.setMaxSourceBytes(48);
        properties.setMaxTotalSourceBytes(80);
        DynamicEntityRegistry registry = registry(compiler, properties);
        try {
            assertFalse(registry.rebuild(Collections.singletonList(entity("large", "LargeEntity",
                    "public class LargeEntity { public String value = \"中文中文中文中文\"; }"))));
            assertEquals("source.too-large", registry.status().getDiagnostics().get(0).getCode());
            assertEquals(0, compiler.invocations.get());

            properties.setMaxSourceBytes(80);
            assertFalse(registry.rebuild(Arrays.asList(
                    entity("one", "One", "public class One { public String value; }"),
                    entity("two", "Two", "public class Two { public String value; }"))));
            assertEquals("generation.too-large", registry.status().getDiagnostics().get(0).getCode());
            assertEquals(0, compiler.invocations.get());
        } finally {
            registry.close();
        }
    }

    @Test
    void cancelsTimedOutCompilationAndKeepsTheCurrentGeneration() {
        JavaEntityProperties properties = new JavaEntityProperties();
        properties.setCompileTimeout(Duration.ofSeconds(30L));
        SwitchingCompiler compiler = new SwitchingCompiler();
        DynamicEntityRegistry registry = registry(compiler, properties);
        try {
            assertTrue(registry.rebuild(Collections.singletonList(entity("user", "User",
                    "public class User { public String oldField; }"))));
            Class<?> oldClass = registry.resolve("User").javaClass();
            compiler.block = true;
            properties.setCompileTimeout(Duration.ofMillis(50L));
            long started = System.nanoTime();

            assertFalse(registry.rebuild(Collections.singletonList(entity("user", "User",
                    "public class User { public String newField; }"))));

            assertEquals("compiler.timeout", registry.status().getDiagnostics().get(0).getCode());
            assertTrue(registry.status().isAvailable());
            assertSame(oldClass, registry.resolve("User").javaClass());
            assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(2L)) < 0);
        } finally {
            registry.close();
        }
    }

    @Test
    void timeoutDoesNotBlockTheNextRealJavaCompilation() {
        StallingJavaCompiler javaCompiler = new StallingJavaCompiler(ToolProvider.getSystemJavaCompiler());
        InMemoryJavaEntityCompiler compiler = new InMemoryJavaEntityCompiler(
                javaCompiler, DynamicJavaEntityIntegrationTest.class.getClassLoader());
        JavaEntityProperties properties = new AwaitingTimeoutProperties(javaCompiler);
        properties.setMaxConcurrentCompilations(2);
        DynamicEntityRegistry registry = registry(compiler, properties);
        try {
            assertFalse(registry.rebuild(Collections.singletonList(entity("first", "First",
                    "public class First {}"))));
            assertTrue(javaCompiler.firstTaskStarted());
            assertEquals("compiler.timeout", registry.status().getDiagnostics().get(0).getCode());

            properties.setCompileTimeout(Duration.ofSeconds(30L));
            assertTrue(registry.rebuild(Collections.singletonList(entity("second", "Second",
                    "public class Second { public String value; }"))),
                    () -> registry.status().getDiagnostics().isEmpty() ? "无诊断"
                            : registry.status().getDiagnostics().get(0).getCode() + ": "
                            + registry.status().getDiagnostics().get(0).getMessage());
            assertEquals("value", registry.resolve("Second").javaClass().getField("value").getName());
        } catch (NoSuchFieldException exception) {
            throw new AssertionError(exception);
        } finally {
            javaCompiler.releaseFirstTask();
            registry.close();
        }
    }

    @Test
    void rejectsNewCompilationWhileTimedOutTaskConsumesTheConcurrencyLimit() {
        StallingJavaCompiler javaCompiler = new StallingJavaCompiler(ToolProvider.getSystemJavaCompiler());
        InMemoryJavaEntityCompiler compiler = new InMemoryJavaEntityCompiler(
                javaCompiler, DynamicJavaEntityIntegrationTest.class.getClassLoader());
        JavaEntityProperties properties = new AwaitingTimeoutProperties(javaCompiler);
        DynamicEntityRegistry registry = registry(compiler, properties);
        try {
            assertFalse(registry.rebuild(Collections.singletonList(entity("first", "First",
                    "public class First {}"))));
            assertEquals("compiler.timeout", registry.status().getDiagnostics().get(0).getCode());

            properties.setCompileTimeout(Duration.ofSeconds(30L));
            assertFalse(registry.rebuild(Collections.singletonList(entity("second", "Second",
                    "public class Second {}"))));
            assertEquals("compiler.busy", registry.status().getDiagnostics().get(0).getCode());
            assertEquals(0L, registry.currentVersion());
        } finally {
            javaCompiler.releaseFirstTask();
            registry.close();
        }
    }

    @Test
    void truncatesDiagnosticCountAndMessageLength() {
        JavaEntityProperties properties = new JavaEntityProperties();
        properties.setMaxDiagnostics(2);
        properties.setMaxDiagnosticMessageLength(12);
        DynamicEntityRegistry registry = registry(new DiagnosticCompiler(), properties);
        try {
            assertFalse(registry.rebuild(Collections.singletonList(entity("user", "User",
                    "public class User {}"))));

            assertEquals(2, registry.status().getDiagnostics().size());
            assertEquals("first-diagno", registry.status().getDiagnostics().get(0).getMessage());
            assertEquals("second-diagn", registry.status().getDiagnostics().get(1).getMessage());
        } finally {
            registry.close();
        }
    }

    @Test
    void roundTripsResourceCreatesEntityAndHotUpdatesWithoutInvalidatingOldDescriptor() throws Exception {
        JavaEntityProperties properties = new JavaEntityProperties();
        DynamicEntityRegistry entityRegistry = registry(new InMemoryJavaEntityCompiler(), properties);
        JavaEntityMagicResourceStorage storage =
                new JavaEntityMagicResourceStorage(new JavaSourceAnalyzer(), entityRegistry);
        JavaEntityMagicDynamicRegistry resourceRegistry =
                new JavaEntityMagicDynamicRegistry(storage, entityRegistry);
        EntityModule module = new EntityModule(entityRegistry);
        try {
            JavaEntityInfo firstResource = storage.read(storage.write(entity("user", "User",
                    "package demo; public class User {\n"
                            + "  private String name;\n"
                            + "  public void setName(String value) { this.name = value; }\n"
                            + "  public String getName() { return name; }\n"
                            + "}")));
            assertTrue(resourceRegistry.register(firstResource));
            DynamicEntityDescriptor first = module.getDynamicAttribute("User");
            Object firstUser = first.create(Collections.<String, Object>singletonMap("name", "Alice"));
            assertEquals("Alice", firstUser.getClass().getMethod("getName").invoke(firstUser));

            JavaEntityInfo secondResource = entity("user", "User",
                    "package demo; public class User {\n"
                            + "  public String displayName;\n"
                            + "}");
            assertTrue(resourceRegistry.register(secondResource));
            DynamicEntityDescriptor second = module.getDynamicAttribute("User");

            assertEquals(1L, first.version());
            assertEquals(2L, second.version());
            assertNotSame(first.javaClass(), second.javaClass());
            assertSame(first.javaClass(), first.create().getClass());
            assertEquals("Bob", second.javaClass().getField("displayName")
                    .get(second.create(Collections.<String, Object>singletonMap("displayName", "Bob"))));
        } finally {
            entityRegistry.close();
        }
    }

    private static DynamicEntityRegistry registry(InMemoryJavaEntityCompiler compiler,
                                                   JavaEntityProperties properties) {
        return new DynamicEntityRegistry(new JavaSourceAnalyzer(), compiler, properties);
    }

    private static JavaEntityInfo entity(String id, String name, String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return info;
    }

    private static class RecordingCompiler extends InMemoryJavaEntityCompiler {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public CompiledGeneration compile(long version, List<JavaSourceUnit> sourceUnits) {
            invocations.incrementAndGet();
            return super.compile(version, sourceUnits);
        }
    }

    private static final class SwitchingCompiler extends InMemoryJavaEntityCompiler {

        private volatile boolean block;

        @Override
        public CompiledGeneration compile(long version, List<JavaSourceUnit> sourceUnits) {
            if (!block) {
                return super.compile(version, sourceUnits);
            }
            try {
                Thread.sleep(10000L);
                throw new AssertionError("编译任务未被取消");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new JavaEntityCompilationException(new JavaEntityDiagnostic(
                        "", 0, 0, "compiler.interrupted", "编译任务已取消"));
            }
        }
    }

    private static final class DiagnosticCompiler extends InMemoryJavaEntityCompiler {

        @Override
        public CompiledGeneration compile(long version, List<JavaSourceUnit> sourceUnits) {
            throw new JavaEntityCompilationException(Arrays.asList(
                    diagnostic("first-diagnostic-message"),
                    diagnostic("second-diagnostic-message"),
                    diagnostic("third-diagnostic-message")));
        }

        private static JavaEntityDiagnostic diagnostic(String message) {
            return new JavaEntityDiagnostic("user", 1, 1, "compiler.test", message);
        }
    }

    private static final class StallingJavaCompiler implements JavaCompiler {

        private final JavaCompiler delegate;
        private final AtomicBoolean stallNextTask = new AtomicBoolean(true);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch firstTaskStarted = new CountDownLatch(1);

        private StallingJavaCompiler(JavaCompiler delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompilationTask getTask(Writer out, JavaFileManager fileManager,
                                       DiagnosticListener<? super JavaFileObject> diagnosticListener,
                                       Iterable<String> options, Iterable<String> classes,
                                       Iterable<? extends JavaFileObject> compilationUnits) {
            CompilationTask task = delegate.getTask(
                    out, fileManager, diagnosticListener, options, classes, compilationUnits);
            return stallNextTask.compareAndSet(true, false)
                    ? new StallingCompilationTask(task, release, firstTaskStarted) : task;
        }

        @Override
        public StandardJavaFileManager getStandardFileManager(
                DiagnosticListener<? super JavaFileObject> diagnosticListener,
                Locale locale, Charset charset) {
            return delegate.getStandardFileManager(diagnosticListener, locale, charset);
        }

        @Override
        public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
            return delegate.run(in, out, err, arguments);
        }

        @Override
        public Set<SourceVersion> getSourceVersions() {
            return delegate.getSourceVersions();
        }

        @Override
        public int isSupportedOption(String option) {
            return delegate.isSupportedOption(option);
        }

        private void releaseFirstTask() {
            release.countDown();
        }

        private boolean firstTaskStarted() {
            return firstTaskStarted.getCount() == 0L;
        }

        private boolean awaitFirstTaskStarted(long timeout, TimeUnit unit) {
            try {
                return firstTaskStarted.await(timeout, unit);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static final class AwaitingTimeoutProperties extends JavaEntityProperties {

        private final StallingJavaCompiler compiler;
        private final AtomicBoolean firstTimeout = new AtomicBoolean(true);

        private AwaitingTimeoutProperties(StallingJavaCompiler compiler) {
            this.compiler = compiler;
            setCompileTimeout(Duration.ofSeconds(30L));
        }

        @Override
        public Duration getCompileTimeout() {
            if (firstTimeout.compareAndSet(true, false)) {
                if (!compiler.awaitFirstTaskStarted(30L, TimeUnit.SECONDS)) {
                    throw new AssertionError("首个真实 JavaCompiler 任务未在 30 秒内启动");
                }
                return Duration.ofMillis(50L);
            }
            return super.getCompileTimeout();
        }
    }

    private static final class StallingCompilationTask implements JavaCompiler.CompilationTask {

        private final JavaCompiler.CompilationTask delegate;
        private final CountDownLatch release;
        private final CountDownLatch started;

        private StallingCompilationTask(JavaCompiler.CompilationTask delegate, CountDownLatch release,
                                        CountDownLatch started) {
            this.delegate = delegate;
            this.release = release;
            this.started = started;
        }

        @Override
        public void setProcessors(Iterable<? extends Processor> processors) {
            delegate.setProcessors(processors);
        }

        @Override
        public void setLocale(Locale locale) {
            delegate.setLocale(locale);
        }

        @Override
        public Boolean call() {
            started.countDown();
            boolean interrupted = false;
            while (true) {
                try {
                    release.await();
                    break;
                } catch (InterruptedException exception) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return Boolean.FALSE;
        }
    }
}
