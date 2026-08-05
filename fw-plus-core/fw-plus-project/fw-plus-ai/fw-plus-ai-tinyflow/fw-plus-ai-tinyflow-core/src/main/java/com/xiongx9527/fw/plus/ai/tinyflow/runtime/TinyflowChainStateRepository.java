package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

import dev.tinyflow.core.chain.ChainState;
import dev.tinyflow.core.chain.repository.ChainStateRepository;

public interface TinyflowChainStateRepository extends ChainStateRepository {

	ChainState find(String instanceId);
}
