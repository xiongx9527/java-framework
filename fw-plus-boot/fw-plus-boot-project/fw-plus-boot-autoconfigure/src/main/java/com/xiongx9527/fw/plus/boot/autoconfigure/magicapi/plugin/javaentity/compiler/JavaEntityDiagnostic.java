package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

public final class JavaEntityDiagnostic {

    private final String resourceId;
    private final long line;
    private final long column;
    private final String code;
    private final String message;

    public JavaEntityDiagnostic(String resourceId, long line, long column, String code, String message) {
        this.resourceId = resourceId;
        this.line = line;
        this.column = column;
        this.code = code;
        this.message = message;
    }

    public String getResourceId() {
        return resourceId;
    }

    public long getLine() {
        return line;
    }

    public long getColumn() {
        return column;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
