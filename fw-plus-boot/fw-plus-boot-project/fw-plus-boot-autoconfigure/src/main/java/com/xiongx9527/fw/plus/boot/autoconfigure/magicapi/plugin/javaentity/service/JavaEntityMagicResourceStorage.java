package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityCompilationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

public class JavaEntityMagicResourceStorage implements MagicResourceStorage<JavaEntityInfo> {

    public static final String RESOURCE_TYPE = "java-entity";

    private final JavaSourceAnalyzer analyzer;

    public JavaEntityMagicResourceStorage(JavaSourceAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @Override
    public String folder() {
        return RESOURCE_TYPE;
    }

    @Override
    public String suffix() {
        return ".java";
    }

    @Override
    public Class<JavaEntityInfo> magicClass() {
        return JavaEntityInfo.class;
    }

    @Override
    public boolean requirePath() {
        return false;
    }

    @Override
    public boolean requiredScript() {
        return true;
    }

    @Override
    public boolean allowRoot() {
        return true;
    }

    @Override
    public String buildMappingKey(JavaEntityInfo entity) {
        return analyzer.analyze(entity).getClassName();
    }

    @Override
    public void validate(JavaEntityInfo entity) {
        try {
            analyzer.analyze(entity);
        } catch (JavaEntityCompilationException exception) {
            String message = exception.getDiagnostics().isEmpty()
                    ? "动态实体源码无效" : exception.getDiagnostics().get(0).getMessage();
            throw new IllegalArgumentException(message, exception);
        }
    }

    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
    }
}
