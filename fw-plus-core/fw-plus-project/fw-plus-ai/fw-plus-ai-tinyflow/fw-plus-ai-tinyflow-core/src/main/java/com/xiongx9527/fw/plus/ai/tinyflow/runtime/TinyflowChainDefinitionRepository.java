package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

import dev.tinyflow.core.chain.ChainDefinition;
import dev.tinyflow.core.chain.repository.ChainDefinitionRepository;

public interface TinyflowChainDefinitionRepository extends ChainDefinitionRepository {

	void register(ChainDefinition definition);

	void remove(String id);
}
