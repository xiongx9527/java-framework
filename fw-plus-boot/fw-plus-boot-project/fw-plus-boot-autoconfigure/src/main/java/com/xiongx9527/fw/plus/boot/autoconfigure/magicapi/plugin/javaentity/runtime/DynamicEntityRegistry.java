package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.CompiledGeneration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityCompilationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceUnit;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class DynamicEntityRegistry {

    private final JavaSourceAnalyzer analyzer;
    private final InMemoryJavaEntityCompiler compiler;
    private final ReentrantLock rebuildLock = new ReentrantLock();
    private final AtomicReference<DynamicEntityGeneration> current =
            new AtomicReference<>(DynamicEntityGeneration.empty());
    private final AtomicReference<DynamicEntityStatus> status =
            new AtomicReference<>(DynamicEntityStatus.success(0L));
    private final Map<String, JavaEntityInfo> successfulSources = new LinkedHashMap<>();

    public DynamicEntityRegistry(JavaSourceAnalyzer analyzer, InMemoryJavaEntityCompiler compiler) {
        this.analyzer = analyzer;
        this.compiler = compiler;
        if (!compiler.isAvailable()) {
            status.set(DynamicEntityStatus.unavailable());
        }
    }

    public boolean rebuild(Collection<JavaEntityInfo> entities) {
        rebuildLock.lock();
        try {
            List<JavaEntityInfo> snapshot = copy(entities);
            List<JavaSourceUnit> units = new ArrayList<>();
            Map<String, JavaSourceUnit> byClassName = new LinkedHashMap<>();
            Map<String, Integer> aliasCounts = new LinkedHashMap<>();
            Map<String, String> aliases = new LinkedHashMap<>();
            for (JavaEntityInfo entity : snapshot) {
                JavaSourceUnit unit = analyzer.analyze(entity);
                JavaSourceUnit duplicateClass = byClassName.put(unit.getClassName(), unit);
                if (duplicateClass != null) {
                    return fail("entity.class.duplicate", unit,
                            "动态实体全限定类名重复: " + unit.getClassName());
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

            long nextVersion = current.get().getVersion() + 1L;
            CompiledGeneration compiled = compiler.compile(nextVersion, units);
            DynamicEntityGeneration next = DynamicEntityGeneration.from(compiled, units, aliases);
            current.set(next);
            successfulSources.clear();
            for (JavaEntityInfo entity : snapshot) {
                successfulSources.put(entity.getId(), entity);
            }
            status.set(DynamicEntityStatus.success(nextVersion));
            return true;
        } catch (JavaEntityCompilationException exception) {
            status.set(DynamicEntityStatus.failure(current.get().getVersion(), exception.getDiagnostics()));
            return false;
        } catch (RuntimeException exception) {
            JavaEntityDiagnostic diagnostic = new JavaEntityDiagnostic("", 0, 0,
                    "entity.registry.internal", safeMessage(exception));
            status.set(DynamicEntityStatus.failure(current.get().getVersion(),
                    Collections.singletonList(diagnostic)));
            return false;
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
        return current.get().resolve(name);
    }

    public long currentVersion() {
        return current.get().getVersion();
    }

    public DynamicEntityStatus status() {
        return status.get();
    }

    private boolean fail(String code, JavaSourceUnit unit, String message) {
        JavaEntityDiagnostic diagnostic = new JavaEntityDiagnostic(
                unit.getResourceId(), 0, 0, code, message);
        status.set(DynamicEntityStatus.failure(current.get().getVersion(),
                Collections.singletonList(diagnostic)));
        return false;
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

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = exception.getClass().getSimpleName();
        }
        message = message.replace('\r', ' ').replace('\n', ' ');
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
