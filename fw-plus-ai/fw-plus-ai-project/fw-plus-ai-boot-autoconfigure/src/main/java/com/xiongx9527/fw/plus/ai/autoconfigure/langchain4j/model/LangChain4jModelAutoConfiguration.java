package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.DefaultAiModelFactory;
import com.xiongx9527.fw.plus.ai.model.DefaultAiModelRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnClass(AiModelRegistry.class)
@EnableConfigurationProperties(AiModelProperties.class)
public class LangChain4jModelAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AiModelFactory.class)
	public AiModelFactory aiModelFactory() {
		return new DefaultAiModelFactory();
	}

	@Bean
	@ConditionalOnMissingBean(AiModelRegistry.class)
	public AiModelRegistry aiModelRegistry(AiModelProperties properties,
										   AiModelFactory factory,
										   List<AiModelAdapter> adapters) {
		Map<String, AiModelDefinition> definitions = definitions(properties);
		Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
		for (AiModelDefinition definition : definitions.values()) {
			models.put(definition.getKey(), factory.create(definition, adapters));
		}
		return new DefaultAiModelRegistry(models, properties.isEnabled() ? properties.getDefaultModel() : null);
	}

	private Map<String, AiModelDefinition> definitions(AiModelProperties properties) {
		Map<String, AiModelDefinition> definitions = new LinkedHashMap<String, AiModelDefinition>();
		if (!properties.isEnabled()) {
			return definitions;
		}
		for (Map.Entry<String, AiModelProperties.Model> entry : properties.getModels().entrySet()) {
			AiModelDefinition definition = entry.getValue();
			if (definition == null) {
				continue;
			}
			if (isBlank(definition.getKey())) {
				definition.setKey(entry.getKey());
			}
			AiModelDefinition existing = definitions.get(definition.getKey());
			if (existing != null && existing.isEnabled() && definition.isEnabled()) {
				throw new AiModelException("Duplicate enabled model key: " + definition.getKey());
			}
			if (definition.isEnabled()) {
				definitions.put(definition.getKey(), definition);
			}
		}
		return definitions;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
