package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DynamicEntityStatus {

    private final boolean available;
    private final long version;
    private final List<JavaEntityDiagnostic> diagnostics;

    private DynamicEntityStatus(boolean available, long version, List<JavaEntityDiagnostic> diagnostics) {
        this.available = available;
        this.version = version;
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    static DynamicEntityStatus success(long version) {
        return new DynamicEntityStatus(true, version, Collections.<JavaEntityDiagnostic>emptyList());
    }

    static DynamicEntityStatus failure(long version, List<JavaEntityDiagnostic> diagnostics) {
        return new DynamicEntityStatus(version > 0L, version, diagnostics);
    }

    static DynamicEntityStatus unavailable() {
        return new DynamicEntityStatus(false, 0L, Collections.singletonList(
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
