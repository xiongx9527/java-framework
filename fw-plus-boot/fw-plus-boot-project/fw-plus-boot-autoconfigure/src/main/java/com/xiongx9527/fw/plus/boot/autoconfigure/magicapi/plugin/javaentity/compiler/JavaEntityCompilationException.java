package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JavaEntityCompilationException extends RuntimeException {

    private final List<JavaEntityDiagnostic> diagnostics;

    public JavaEntityCompilationException(JavaEntityDiagnostic diagnostic) {
        this(Collections.singletonList(diagnostic));
    }

    public JavaEntityCompilationException(List<JavaEntityDiagnostic> diagnostics) {
        super(firstMessage(diagnostics));
        this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    public List<JavaEntityDiagnostic> getDiagnostics() {
        return diagnostics;
    }

    private static String firstMessage(List<JavaEntityDiagnostic> diagnostics) {
        return diagnostics == null || diagnostics.isEmpty()
                ? "动态实体编译失败"
                : diagnostics.get(0).getMessage();
    }
}
