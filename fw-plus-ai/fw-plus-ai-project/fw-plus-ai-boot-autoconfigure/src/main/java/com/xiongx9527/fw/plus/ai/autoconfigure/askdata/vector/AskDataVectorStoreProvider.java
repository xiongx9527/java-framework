package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityDescriptor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorStore;

/** 将中立向量存储实例注册为可按 key 选择的 Core 能力。 */
public final class AskDataVectorStoreProvider implements AskDataCapabilityProvider {

	private final AskDataCapabilityDescriptor descriptor;
	private final AskDataVectorStore store;

	public AskDataVectorStoreProvider(String key, String configurationNamespace, boolean clusterCompatible,
			AskDataVectorStore store) {
		if (store == null) {
			throw new IllegalArgumentException("store 不能为空");
		}
		this.descriptor = new AskDataCapabilityDescriptor(key, AskDataCapabilityType.VECTOR,
				"0.1.1-SNAPSHOT", "0.1.1", "0.1.1", 0, clusterCompatible, configurationNamespace);
		this.store = store;
	}

	@Override
	public AskDataCapabilityDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public Object getCapability() {
		return store;
	}
}
