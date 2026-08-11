package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.CompiledGeneration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityCompilationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceUnit;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.starter.JavaEntityProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class DynamicEntityRegistry implements AutoCloseable {

    private final JavaSourceAnalyzer analyzer;
    private final InMemoryJavaEntityCompiler compiler;
    private final JavaEntityProperties properties;
    private ExecutorService compileExecutor;
    private final ReentrantLock rebuildLock = new ReentrantLock();
    private final AtomicReference<DynamicEntityGeneration> current =
            new AtomicReference<>(DynamicEntityGeneration.empty());
    private final AtomicReference<DynamicEntityStatus> status =
            new AtomicReference<>(DynamicEntityStatus.success(0L));
    private final Map<String, JavaEntityInfo> successfulSources = new LinkedHashMap<>();

    public DynamicEntityRegistry(JavaSourceAnalyzer analyzer, InMemoryJavaEntityCompiler compiler) {
        this(analyzer, compiler, new JavaEntityProperties());
    }

    public DynamicEntityRegistry(JavaSourceAnalyzer analyzer, InMemoryJavaEntityCompiler compiler,
                                 JavaEntityProperties properties) {
        this.analyzer = analyzer;
        this.compiler = compiler;
        this.properties = properties;
        this.compileExecutor = newCompileExecutor();
        if (!compiler.isAvailable()) {
            status.set(DynamicEntityStatus.unavailable());
        }
    }

    public boolean rebuild(Collection<JavaEntityInfo> entities) {
        rebuildLock.lock();
        try {
            List<JavaEntityInfo> snapshot = copy(entities);
            CandidateSources candidate = prepareSources(snapshot);

            long nextVersion = current.get().getVersion() + 1L;
            CompiledGeneration compiled = compile(nextVersion, candidate.units);
            DynamicEntityGeneration next;
            try {
                next = DynamicEntityGeneration.from(compiled, candidate.units, candidate.aliases);
            } catch (RuntimeException exception) {
                compiled.close();
                throw exception;
            }
            current.set(next);
            successfulSources.clear();
            for (JavaEntityInfo entity : snapshot) {
                successfulSources.put(entity.getId(), entity);
            }
            status.set(DynamicEntityStatus.success(nextVersion));
            return true;
        } catch (JavaEntityCompilationException exception) {
            setFailure(exception.getDiagnostics());
            return false;
        } catch (RuntimeException exception) {
            JavaEntityDiagnostic diagnostic = new JavaEntityDiagnostic("", 0, 0,
                    "entity.registry.internal", safeMessage(exception));
            setFailure(Collections.singletonList(diagnostic));
            return false;
        } finally {
            rebuildLock.unlock();
        }
    }

    public void validateCandidate(Collection<JavaEntityInfo> entities) {
        rebuildLock.lock();
        try {
            CandidateSources candidate = prepareSources(copy(entities));
            CompiledGeneration compiled = compile(current.get().getVersion() + 1L, candidate.units);
            compiled.close();
        } catch (JavaEntityCompilationException exception) {
            throw new JavaEntityCompilationException(limitDiagnostics(exception.getDiagnostics()));
        } catch (RuntimeException exception) {
            throw new JavaEntityCompilationException(new JavaEntityDiagnostic("", 0, 0,
                    "entity.registry.internal", safeMessage(exception)));
        } finally {
            rebuildLock.unlock();
        }
    }

    public boolean replace(JavaEntityInfo entity) {
        rebuildLock.lock();
        try {
            Map<String, JavaEntityInfo> candidate = new LinkedHashMap<>(successfulSources);
            candidate.put(entity.getId(), entity.copy());
            return rebuild(candidate.values());
        } finally {
            rebuildLock.unlock();
        }
    }

    public boolean remove(String resourceId) {
        rebuildLock.lock();
        try {
            if (!successfulSources.containsKey(resourceId)) {
                return false;
            }
            Map<String, JavaEntityInfo> candidate = new LinkedHashMap<>(successfulSources);
            candidate.remove(resourceId);
            return rebuild(candidate.values());
        } finally {
            rebuildLock.unlock();
        }
    }

    public DynamicEntityDescriptor resolve(String name) {
        try {
            return current.get().resolve(name);
        } catch (IllegalArgumentException exception) {
            DynamicEntityStatus currentStatus = status.get();
            if (!currentStatus.isAvailable()) {
                String reason = currentStatus.getDiagnostics().isEmpty()
                        ? "无可用代际" : currentStatus.getDiagnostics().get(0).getMessage();
                throw new IllegalStateException("动态实体不可用: " + reason, exception);
            }
            throw exception;
        }
    }

    public long currentVersion() {
        return current.get().getVersion();
    }

    public DynamicEntityStatus status() {
        return status.get();
    }

    @Override
    public void close() {
        rebuildLock.lock();
        try {
            compileExecutor.shutdownNow();
        } finally {
            rebuildLock.unlock();
        }
    }

    private CompiledGeneration compile(long version, List<JavaSourceUnit> units) {
        Future<CompiledGeneration> future = compileExecutor.submit(() -> compiler.compile(version, units));
        Duration timeout = properties.getCompileTimeout();
        long timeoutMillis = timeout == null ? 30000L : Math.max(1L, timeout.toMillis());
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            rotateCompileExecutor();
            throw new JavaEntityCompilationException(new JavaEntityDiagnostic("", 0, 0,
                    "compiler.timeout", "动态实体编译超过限制时间 " + timeoutMillis + "ms"));
        } catch (InterruptedException exception) {
            future.cancel(true);
            rotateCompileExecutor();
            Thread.currentThread().interrupt();
            throw new JavaEntityCompilationException(new JavaEntityDiagnostic("", 0, 0,
                    "compiler.interrupted", "动态实体编译等待被中断"));
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof JavaEntityCompilationException) {
                throw (JavaEntityCompilationException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new JavaEntityCompilationException(new JavaEntityDiagnostic("", 0, 0,
                    "compiler.internal", safeMessage(cause)));
        }
    }

    private void rotateCompileExecutor() {
        ExecutorService retired = compileExecutor;
        compileExecutor = newCompileExecutor();
        retired.shutdownNow();
    }

    private static ExecutorService newCompileExecutor() {
        return Executors.newSingleThreadExecutor(new CompilerThreadFactory());
    }

    private CandidateSources prepareSources(List<JavaEntityInfo> entities) {
        validateSourceLimits(entities);
        List<JavaSourceUnit> units = new ArrayList<>();
        Map<String, JavaSourceUnit> byClassName = new LinkedHashMap<>();
        Map<String, Integer> aliasCounts = new LinkedHashMap<>();
        Map<String, String> aliases = new LinkedHashMap<>();
        for (JavaEntityInfo entity : entities) {
            JavaSourceUnit unit = analyzer.analyze(entity);
            JavaSourceUnit duplicateClass = byClassName.put(unit.getClassName(), unit);
            if (duplicateClass != null) {
                throw new JavaEntityCompilationException(new JavaEntityDiagnostic(
                        unit.getResourceId(), 0, 0, "entity.class.duplicate",
                        "动态实体全限定类名重复: " + unit.getClassName()));
            }
            Integer count = aliasCounts.get(unit.getSimpleName());
            aliasCounts.put(unit.getSimpleName(), count == null ? 1 : count + 1);
            units.add(unit);
        }
        for (JavaSourceUnit unit : units) {
            if (Integer.valueOf(1).equals(aliasCounts.get(unit.getSimpleName()))) {
                aliases.put(unit.getSimpleName(), unit.getClassName());
            }
        }
        return new CandidateSources(units, aliases);
    }

    private void validateSourceLimits(List<JavaEntityInfo> entities) {
        long totalBytes = 0L;
        long maxSourceBytes = Math.max(0L, properties.getMaxSourceBytes());
        for (JavaEntityInfo entity : entities) {
            String source = entity.getScript() == null ? "" : entity.getScript();
            long sourceBytes = source.getBytes(StandardCharsets.UTF_8).length;
            if (sourceBytes > maxSourceBytes) {
                throw new JavaEntityCompilationException(new JavaEntityDiagnostic(entity.getId(), 0, 0,
                        "source.too-large", "动态实体源码超过限制: " + sourceBytes + " bytes"));
            }
            totalBytes += sourceBytes;
        }
        if (totalBytes > Math.max(0L, properties.getMaxTotalSourceBytes())) {
            throw new JavaEntityCompilationException(new JavaEntityDiagnostic("", 0, 0,
                    "generation.too-large", "动态实体总代码量超过限制: " + totalBytes + " bytes"));
        }
    }

    private void setFailure(List<JavaEntityDiagnostic> diagnostics) {
        status.set(DynamicEntityStatus.failure(current.get().getVersion(), limitDiagnostics(diagnostics)));
    }

    private List<JavaEntityDiagnostic> limitDiagnostics(List<JavaEntityDiagnostic> diagnostics) {
        List<JavaEntityDiagnostic> limited = new ArrayList<>();
        int maxDiagnostics = Math.max(1, properties.getMaxDiagnostics());
        int maxMessageLength = Math.max(1, properties.getMaxDiagnosticMessageLength());
        if (diagnostics == null) {
            return limited;
        }
        for (JavaEntityDiagnostic diagnostic : diagnostics) {
            if (limited.size() >= maxDiagnostics) {
                break;
            }
            String message = diagnostic.getMessage() == null ? "" : diagnostic.getMessage();
            message = message.replace('\r', ' ').replace('\n', ' ');
            if (message.length() > maxMessageLength) {
                message = message.substring(0, maxMessageLength);
            }
            limited.add(new JavaEntityDiagnostic(diagnostic.getResourceId(), diagnostic.getLine(),
                    diagnostic.getColumn(), diagnostic.getCode(), message));
        }
        return limited;
    }

    private static List<JavaEntityInfo> copy(Collection<JavaEntityInfo> entities) {
        List<JavaEntityInfo> result = new ArrayList<>();
        if (entities != null) {
            for (JavaEntityInfo entity : entities) {
                result.add(entity.copy());
            }
        }
        return result;
    }

    private String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = exception.getClass().getSimpleName();
        }
        message = message.replace('\r', ' ').replace('\n', ' ');
        int maxLength = Math.max(1, properties.getMaxDiagnosticMessageLength());
        return message.length() <= maxLength ? message : message.substring(0, maxLength);
    }

    private static final class CompilerThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "magicapi-java-entity-compiler");
            thread.setDaemon(true);
            return thread;
        }
    }

    private static final class CandidateSources {

        private final List<JavaSourceUnit> units;
        private final Map<String, String> aliases;

        private CandidateSources(List<JavaSourceUnit> units, Map<String, String> aliases) {
            this.units = units;
            this.aliases = aliases;
        }
    }
}
