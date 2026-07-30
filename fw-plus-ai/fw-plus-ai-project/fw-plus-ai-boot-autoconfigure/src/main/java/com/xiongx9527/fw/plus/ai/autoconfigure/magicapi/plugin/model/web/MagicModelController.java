package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class MagicModelController extends MagicController implements MagicExceptionHandler {

    private final MagicDynamicModelDataSource dataSource;
    private final AiModelFactory modelFactory;
    private final List<AiModelAdapter> providerAdapters;

    public MagicModelController(MagicConfiguration configuration) {
        this(configuration, null, null, null);
    }

    public MagicModelController(MagicConfiguration configuration, AiModelFactory modelFactory,
                                List<AiModelAdapter> providerAdapters) {
        this(configuration, null, modelFactory, providerAdapters);
    }

    public MagicModelController(MagicConfiguration configuration, MagicDynamicModelDataSource dataSource,
                                AiModelFactory modelFactory,
                                List<AiModelAdapter> providerAdapters) {
        super(configuration);
        this.dataSource = dataSource;
        this.modelFactory = modelFactory;
        this.providerAdapters = providerAdapters;
    }

    @RequestMapping("/model/datasource/list")
    @ResponseBody
    public JsonBean<List<ModelDataSourceInfo>> list() {
        List<ModelDataSourceInfo> result = new ArrayList<ModelDataSourceInfo>();
        for (ModelDataSourceInfo info : getDataSource().datasourceNodes()) {
            result.add(info.simple());
        }
        return new JsonBean<List<ModelDataSourceInfo>>(result);
    }

    @RequestMapping("/model/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody ModelDataSourceInfo definition) {
        try {
            validate(definition);
            for (AiModelDefinition modelDefinition : definitions(definition)) {
                getModelFactory().create(modelDefinition, getProviderAdapters());
            }
            return new JsonBean<Object>("ok");
        } catch (AiModelException e) {
            log.error("测试 Model 数据源失败，数据源 key: {}, causeType: {}",
                    safeKey(definition), e.getClass().getName());
            return new JsonBean<Object>(-1, selectionFailureMessage(e.getMessage()), null);
        } catch (Exception e) {
            log.error("测试 Model 数据源失败，数据源 key: {}", safeKey(definition));
            return new JsonBean<Object>(-1, "模型数据源测试失败", null);
        }
    }

    private void validate(ModelDataSourceInfo definition) {
        if (definition == null) {
            throw new IllegalArgumentException("model datasource must not be null");
        }
        validateRequired(definition.getKey(), "key");
        validateRequired(definition.getProvider(), "provider");
        if (definition.getModelType() == null) {
            throw new IllegalArgumentException("modelType must not be null");
        }
        List<String> modelNames = definition.modelNames();
        if (modelNames.isEmpty()) {
            throw new IllegalArgumentException("modelName must not be empty");
        }
        for (String modelName : modelNames) {
            validateRequired(modelName, "modelName");
        }
        if (modelNames.size() > 1) {
            String defaultModelName = isBlank(definition.getDefaultModelName())
                    ? modelNames.get(0)
                    : definition.getDefaultModelName().trim();
            if (!modelNames.contains(defaultModelName)) {
                throw new IllegalArgumentException("defaultModelName is invalid");
            }
            ModelDataSourceInfo.ModelConfig defaultConfig = definition.modelConfig(defaultModelName);
            if (defaultConfig != null && Boolean.FALSE.equals(defaultConfig.getEnabled())) {
                throw new IllegalArgumentException("defaultModelName is disabled");
            }
        }
    }

    private void validateRequired(String value, String field) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private MagicDynamicModelDataSource getDataSource() {
        return dataSource == null ? SpringUtils.getBean(MagicDynamicModelDataSource.class) : dataSource;
    }

    private AiModelFactory getModelFactory() {
        return modelFactory == null ? SpringUtils.getBean(AiModelFactory.class) : modelFactory;
    }

    private List<AiModelAdapter> getProviderAdapters() {
        if (providerAdapters != null) {
            return providerAdapters;
        }
        Map<String, AiModelAdapter> adapters = SpringUtils.getBeansOfType(AiModelAdapter.class);
        return adapters == null ? Collections.<AiModelAdapter>emptyList()
                : new ArrayList<AiModelAdapter>(adapters.values());
    }

    private String safeKey(ModelDataSourceInfo definition) {
        return definition == null || definition.getKey() == null ? "unknown" : definition.getKey();
    }

    private String selectionFailureMessage(String message) {
        String candidates = safeCandidateList(message, "candidates=");
        if (candidates == null) {
            return "模型数据源测试失败";
        }
        if (message.startsWith("No adapter supports ")) {
            return "No adapter supports model: candidates=" + candidates;
        }
        if (message.startsWith("Multiple adapters support ")) {
            return "Multiple adapters support model: candidates=" + candidates;
        }
        return "模型数据源测试失败";
    }

    private String safeCandidateList(String message, String marker) {
        if (message == null) {
            return null;
        }
        int markerIndex = message.indexOf(marker);
        int start = markerIndex < 0 ? -1 : markerIndex + marker.length();
        int end = start < 0 ? -1 : message.indexOf(']', start);
        if (start < 0 || end < start) {
            return null;
        }
        String candidates = message.substring(start, end + 1);
        if (!candidates.matches("\\[[A-Za-z0-9._, -]*\\]")) {
            return null;
        }
        return candidates;
    }

    private List<AiModelDefinition> definitions(ModelDataSourceInfo definition) {
        List<String> modelNames = definition.modelNames();
        List<AiModelDefinition> definitions = new ArrayList<AiModelDefinition>();
        if (modelNames.size() <= 1) {
            String modelName = modelNames.isEmpty() ? null : modelNames.get(0);
            definitions.add(definition.toDefinition(definition.getKey(), modelName,
                    definition.modelConfig(modelName)));
            return definitions;
        }
        for (String modelName : modelNames) {
            ModelDataSourceInfo.ModelConfig modelConfig = definition.modelConfig(modelName);
            if (modelConfig != null && Boolean.FALSE.equals(modelConfig.getEnabled())) {
                continue;
            }
            definitions.add(definition.toDefinition(definition.getKey() + "." + modelName, modelName, modelConfig));
        }
        return definitions;
    }
}
