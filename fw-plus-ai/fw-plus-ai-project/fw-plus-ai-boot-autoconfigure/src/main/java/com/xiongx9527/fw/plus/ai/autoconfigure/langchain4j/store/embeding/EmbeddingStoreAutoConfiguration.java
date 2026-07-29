package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnClass(AiEmbeddingStoreRegistry.class)
@EnableConfigurationProperties(AiEmbeddingStoreProperties.class)
public class EmbeddingStoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AiEmbeddingStoreFactory.class)
	public AiEmbeddingStoreFactory aiEmbeddingStoreFactory() {
		return new DefaultAiEmbeddingStoreFactory();
	}

	@Bean
	@ConditionalOnMissingBean(AiEmbeddingStoreRegistry.class)
	public AiEmbeddingStoreRegistry aiEmbeddingStoreRegistry(AiEmbeddingStoreProperties properties,
													   AiEmbeddingStoreFactory factory,
													   List<AiEmbeddingStoreAdapter> adapters) {
		Map<String, AiEmbeddingStoreDefinition> definitions = definitions(properties);
		Map<String, AiEmbeddingStore> stores = new LinkedHashMap<String, AiEmbeddingStore>();
		for (AiEmbeddingStoreDefinition definition : definitions.values()) {
			stores.put(definition.getKey(), factory.create(definition, adapters));
		}
		return new DefaultAiEmbeddingStoreRegistry(stores,
				properties.isEnabled() ? properties.getDefaultStore() : null);
	}

	private Map<String, AiEmbeddingStoreDefinition> definitions(AiEmbeddingStoreProperties properties) {
		Map<String, AiEmbeddingStoreDefinition> definitions =
				new LinkedHashMap<String, AiEmbeddingStoreDefinition>();
		if (!properties.isEnabled()) {
			return definitions;
		}
		for (Map.Entry<String, AiEmbeddingStoreProperties.Store> entry : properties.getStores().entrySet()) {
			AiEmbeddingStoreDefinition definition = entry.getValue();
			if (definition == null) {
				continue;
			}
			if (isBlank(definition.getKey())) {
				definition.setKey(entry.getKey());
			}
			AiEmbeddingStoreDefinition existing = definitions.get(definition.getKey());
			if (existing != null && existing.isEnabled() && definition.isEnabled()) {
				throw new AiEmbeddingStoreException("Duplicate enabled store key: key=" + definition.getKey());
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
