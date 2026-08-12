package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraph;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraphRuntime;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentNodeExecutor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityDescriptor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType;

import java.util.List;

/** 可选 Java LangGraph/LangGraph4j 运行时 Adapter。 */
public final class LangGraph4jAskDataGraphAdapter implements AskDataAgentGraphRuntime, AskDataCapabilityProvider {
	private final String key;
	private final LangGraph4jAskDataTransport transport;
	public LangGraph4jAskDataGraphAdapter(String key, LangGraph4jAskDataTransport transport) {
		if (key == null || key.trim().length() == 0 || transport == null) {
			throw new IllegalArgumentException("key 和 transport 不能为空");
		}
		this.key = key.trim(); this.transport = transport;
	}
	@Override public String getKey() { return key; }
	@Override public boolean isAvailable() { return transport.isAvailable(); }
	@Override public List<String> execute(AskDataAgentGraph graph, AskDataAgentNodeExecutor executor) {
		if (!isAvailable()) { throw new IllegalStateException("LangGraph4j runtime is unavailable"); }
		return transport.execute(graph, executor);
	}
	@Override public AskDataCapabilityDescriptor getDescriptor() {
		return new AskDataCapabilityDescriptor(key, AskDataCapabilityType.RUNTIME_ADAPTER, "0.1.1-SNAPSHOT",
				"0.1.1", "0.1.1", 0, true, "fw.plus.ai.askdata.flow.langgraph4j");
	}
	@Override public Object getCapability() { return this; }
}
