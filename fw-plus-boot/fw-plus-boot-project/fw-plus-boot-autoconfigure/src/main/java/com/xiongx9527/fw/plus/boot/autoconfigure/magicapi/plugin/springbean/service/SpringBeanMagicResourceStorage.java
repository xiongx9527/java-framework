package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityCompilationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.model.SpringBeanInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime.DynamicSpringBeanRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpringBeanMagicResourceStorage implements MagicResourceStorage<SpringBeanInfo> {

    public static final String RESOURCE_TYPE = "spring-bean";

    private final JavaSourceAnalyzer analyzer;
    private final DynamicSpringBeanRegistry registry;
    private MagicResourceService magicResourceService;

    public SpringBeanMagicResourceStorage(JavaSourceAnalyzer analyzer, DynamicSpringBeanRegistry registry) {
        this.analyzer = analyzer;
        this.registry = registry;
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
    public Class<SpringBeanInfo> magicClass() {
        return SpringBeanInfo.class;
    }

    @Override
    public boolean requirePath() {
        return true;
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
    public String buildMappingKey(SpringBeanInfo entity) {
        return analyzer.analyze(entity).getClassName();
    }

    @Override
    public void validate(SpringBeanInfo entity) {
        try {
            registry.validateCandidate(candidateSources(entity));
        } catch (JavaEntityCompilationException exception) {
            throw new IllegalArgumentException(diagnosticMessage(exception), exception);
        }
    }

    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    private List<SpringBeanInfo> candidateSources(SpringBeanInfo candidate) {
        List<SpringBeanInfo> result = new ArrayList<>();
        if (magicResourceService != null) {
            for (SpringBeanInfo existing : magicResourceService.<SpringBeanInfo>files(RESOURCE_TYPE)) {
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
            return "动态 Spring Bean 源码无效";
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
            }
            if (message.length() > 0) {
                message.append(' ');
            }
            message.append(diagnostic.getMessage());
        }
        return message.toString();
    }
}
