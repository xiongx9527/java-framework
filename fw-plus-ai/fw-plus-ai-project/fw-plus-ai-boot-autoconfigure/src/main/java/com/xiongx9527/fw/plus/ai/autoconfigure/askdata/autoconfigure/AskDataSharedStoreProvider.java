package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityDescriptor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataSharedStoreAdapter;

/** 将 JDBC 或 Redis 共享状态适配器注册为统一 STORE 能力。 */
final class AskDataSharedStoreProvider implements AskDataCapabilityProvider {

	private final AskDataCapabilityDescriptor descriptor;
	private final AskDataSharedStoreAdapter stores;

	AskDataSharedStoreProvider(String key, AskDataSharedStoreAdapter stores) {
		if (stores == null) {
			throw new IllegalArgumentException("stores 不能为空");
		}
		this.descriptor = new AskDataCapabilityDescriptor(key, AskDataCapabilityType.STORE,
				"0.1.1-SNAPSHOT", "0.1.1", "0.1.1", 0, true,
				"fw.plus.ai.askdata.state." + key);
		this.stores = stores;
	}

	@Override
	public AskDataCapabilityDescriptor getDescriptor() {
		return descriptor;
	}

	@Override
	public Object getCapability() {
		return stores;
	}
}
