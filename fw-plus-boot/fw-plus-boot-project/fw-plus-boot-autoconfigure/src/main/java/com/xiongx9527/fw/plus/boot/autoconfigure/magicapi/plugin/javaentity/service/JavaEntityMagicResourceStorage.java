package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityCompilationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaEntityMagicResourceStorage implements MagicResourceStorage<JavaEntityInfo> {

    public static final String RESOURCE_TYPE = "java-entity";

    private final JavaSourceAnalyzer analyzer;
    private final DynamicEntityRegistry entityRegistry;
    private MagicResourceService magicResourceService;

    public JavaEntityMagicResourceStorage(JavaSourceAnalyzer analyzer, DynamicEntityRegistry entityRegistry) {
        this.analyzer = analyzer;
        this.entityRegistry = entityRegistry;
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
            entityRegistry.validateCandidate(candidateSources(entity));
        } catch (JavaEntityCompilationException exception) {
            throw new IllegalArgumentException(diagnosticMessage(exception), exception);
        }
    }

    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    private List<JavaEntityInfo> candidateSources(JavaEntityInfo candidate) {
        List<JavaEntityInfo> result = new ArrayList<>();
        if (magicResourceService != null) {
            for (JavaEntityInfo existing : magicResourceService.<JavaEntityInfo>files(RESOURCE_TYPE)) {
                if (!Objects.equals(existing.getId(), candidate.getId())) {
                    result.add(existing.copy());
                }
            }
        }
        result.add(candidate.copy());
        return result;
    }

    private String diagnosticMessage(JavaEntityCompilationException exception) {
        if (exception.getDiagnostics().isEmpty()) {
            return "动态实体源码无效";
        }
        StringBuilder message = new StringBuilder();
        for (JavaEntityDiagnostic diagnostic : exception.getDiagnostics()) {
            if (message.length() > 0) {
                message.append("; ");
            }
            if (diagnostic.getResourceId() != null && !diagnostic.getResourceId().isEmpty()) {
                message.append(diagnostic.getResourceId());
            }
            if (diagnostic.getLine() > 0) {
                message.append(':').append(diagnostic.getLine());
                if (diagnostic.getColumn() > 0) {
                    message.append(':').append(diagnostic.getColumn());
                }
            }
            if (message.length() > 0) {
                message.append(' ');
            }
            message.append(diagnostic.getMessage());
        }
        return message.toString();
    }
}
