package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class JavaEntityCompilationException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码 50029001（SERVER/SCRIPT/001 动态实体 Java 源码编译失败） */
    public static final int CODE = 50_029_001;

    private final List<JavaEntityDiagnostic> diagnostics;

    public JavaEntityCompilationException(JavaEntityDiagnostic diagnostic) {
        this(Collections.singletonList(diagnostic));
    }

    public JavaEntityCompilationException(List<JavaEntityDiagnostic> diagnostics) {
        super(CODE, firstMessage(diagnostics));
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
