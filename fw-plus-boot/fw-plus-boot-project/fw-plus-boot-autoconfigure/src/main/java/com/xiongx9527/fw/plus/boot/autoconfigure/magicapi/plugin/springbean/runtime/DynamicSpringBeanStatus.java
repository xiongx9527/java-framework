package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DynamicSpringBeanStatus {

    private final boolean available;
    private final long version;
    private final List<JavaEntityDiagnostic> diagnostics;

    private DynamicSpringBeanStatus(boolean available, long version, List<JavaEntityDiagnostic> diagnostics) {
        this.available = available;
        this.version = version;
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    static DynamicSpringBeanStatus success(long version) {
        return new DynamicSpringBeanStatus(true, version, Collections.<JavaEntityDiagnostic>emptyList());
    }

    static DynamicSpringBeanStatus failure(long version, List<JavaEntityDiagnostic> diagnostics) {
        return new DynamicSpringBeanStatus(version > 0L, version, diagnostics);
    }

    static DynamicSpringBeanStatus unavailable() {
        return new DynamicSpringBeanStatus(false, 0L, Collections.singletonList(
                new JavaEntityDiagnostic("", 0, 0, "compiler.unavailable",
                        "当前运行环境未提供 JDK JavaCompiler")));
    }

    public boolean isAvailable() {
        return available;
    }

    public long getVersion() {
        return version;
    }

    public List<JavaEntityDiagnostic> getDiagnostics() {
        return diagnostics;
    }
}
