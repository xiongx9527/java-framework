package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(LangChain4jModelProperties.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.langchain4j.model", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jModelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LangChain4jModelFactory.class)
    public LangChain4jModelFactory langChain4jModelFactory() {
        return new DefaultLangChain4jModelFactory();
    }

    @Bean
    @ConditionalOnMissingBean(LangChain4jModelRegistry.class)
    public LangChain4jModelRegistry langChain4jModelRegistry(LangChain4jModelProperties properties,
                                                              LangChain4jModelFactory factory,
                                                              List<LangChain4jModelProviderAdapter> adapters) {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        for (Map.Entry<String, LangChain4jModelProperties.Model> entry : properties.getModels().entrySet()) {
            LangChain4jModelProperties.Model model = entry.getValue();
            if (model == null || !model.isEnabled()) {
                continue;
            }
            LangChain4jModelDefinition definition = definition(entry.getKey(), model);
            models.put(definition.getKey(), factory.create(definition, adapters));
        }
        validateDefaultModel(properties.getDefaultModel(), models);
        return new DefaultLangChain4jModelRegistry(models, properties.getDefaultModel());
    }

    private LangChain4jModelDefinition definition(String key, LangChain4jModelProperties.Model model) {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey(key);
        definition.setProvider(model.getProvider());
        definition.setModelType(model.getModelType());
        definition.setEnabled(model.isEnabled());
        definition.setBaseUrl(model.getBaseUrl());
        definition.setApiKey(model.getApiKey());
        definition.setModelName(model.getModelName());
        definition.setProperties(model.getProperties());
        return definition;
    }

    private void validateDefaultModel(String defaultModel, Map<String, LangChain4jModel> models) {
        if (!isBlank(defaultModel) && !models.containsKey(defaultModel)) {
            throw new LangChain4jModelException("Default model key is invalid: " + defaultModel);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
