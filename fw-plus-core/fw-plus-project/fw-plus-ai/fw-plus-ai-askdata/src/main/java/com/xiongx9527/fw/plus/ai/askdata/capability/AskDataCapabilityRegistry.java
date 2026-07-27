package com.xiongx9527.fw.plus.ai.askdata.capability;

import java.util.List;

/**
 * AskData 能力注册和确定性选择契约。
 */
public interface AskDataCapabilityRegistry {

	void register(AskDataCapabilityProvider provider);

	AskDataCapabilityDescriptor get(String key);

	List<AskDataCapabilityDescriptor> list(AskDataCapabilityType type);
}
