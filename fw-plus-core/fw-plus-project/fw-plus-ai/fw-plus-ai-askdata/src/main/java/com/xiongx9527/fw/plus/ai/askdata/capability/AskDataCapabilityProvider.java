package com.xiongx9527.fw.plus.ai.askdata.capability;

/**
 * AskData 能力提供方。
 *
 * <p>Core 仅通过描述符识别 Provider 的能力，不访问厂商客户端；具体客户端的创建和调用由适配层自行管理。</p>
 */
public interface AskDataCapabilityProvider {

	/**
	 * 返回当前能力的不可变描述符。
	 *
	 * @return 能力描述符，不能为空
	 */
	AskDataCapabilityDescriptor getDescriptor();
}
