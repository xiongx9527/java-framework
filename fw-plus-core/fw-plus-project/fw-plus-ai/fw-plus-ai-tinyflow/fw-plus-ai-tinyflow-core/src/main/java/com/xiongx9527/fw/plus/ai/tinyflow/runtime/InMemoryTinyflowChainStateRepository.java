package com.xiongx9527.fw.plus.ai.tinyflow.runtime;

import dev.tinyflow.core.chain.ChainState;
import dev.tinyflow.core.chain.repository.ChainStateField;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTinyflowChainStateRepository implements TinyflowChainStateRepository {

	private final Map<String, ChainState> states = new ConcurrentHashMap<String, ChainState>();

	@Override
	public ChainState load(String instanceId) {
		ChainState state = states.get(instanceId);
		if (state != null) {
			return state;
		}
		ChainState created = new ChainState();
		created.setInstanceId(instanceId);
		ChainState existing = states.putIfAbsent(instanceId, created);
		return existing == null ? created : existing;
	}

	@Override
	public ChainState find(String instanceId) {
		return states.get(instanceId);
	}

	@Override
	public boolean tryUpdate(ChainState state, EnumSet<ChainStateField> fields) {
		states.put(state.getInstanceId(), state);
		return true;
	}
}
