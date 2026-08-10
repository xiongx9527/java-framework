package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

import dev.tinyflow.core.chain.ChainDefinition;
import dev.tinyflow.core.chain.repository.ChainDefinitionRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChainDefinitionRepository implements ChainDefinitionRepository {

	private final Map<String, ChainDefinition> definitions = new ConcurrentHashMap<String, ChainDefinition>();

	@Override
	public ChainDefinition getChainDefinitionById(String id) {
		return definitions.get(id);
	}

	public void register(ChainDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("definition must not be null");
		}
		if (definition.getId() == null || definition.getId().trim().length() == 0) {
			throw new IllegalArgumentException("definition id must not be blank");
		}
		definitions.put(definition.getId(), definition);
	}

	public void remove(String id) {
		if (id != null) {
			definitions.remove(id);
		}
	}
}
