package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

public final class JavaSourceUnit {

    private final String resourceId;
    private final String resourceName;
    private final String source;
    private final String packageName;
    private final String simpleName;
    private final String className;

    public JavaSourceUnit(String resourceId, String resourceName, String source,
                          String packageName, String simpleName, String className) {
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.source = source;
        this.packageName = packageName;
        this.simpleName = simpleName;
        this.className = className;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getSource() {
        return source;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getClassName() {
        return className;
    }
}
