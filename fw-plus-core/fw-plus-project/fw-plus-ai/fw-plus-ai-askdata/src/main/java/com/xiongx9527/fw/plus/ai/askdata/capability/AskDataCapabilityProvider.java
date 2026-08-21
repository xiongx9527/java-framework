package com.xiongx9527.fw.plus.ai.askdata.capability;

/**
 * AskData 能力提供方。
 *
 * <p>Core 仅通过描述符识别 Provider 的能力，不访问或暴露厂商客户端；具体客户端的创建和调用由适配层自行管理。
 * 因此模型、向量库、SQL 插件和可观测性实现能够在不改变 Core API 的情况下替换。</p>
 */
public interface AskDataCapabilityProvider {

	/**
	 * 返回当前能力的不可变描述符，供 Core 完成版本协商、集群兼容性判断和确定性排序。
	 *
	 * @return 能力描述符，不能为空
	 */
	AskDataCapabilityDescriptor getDescriptor();

	/**
	 * 返回真正执行该能力的中立实现实例。
	 *
	 * <p>Provider 可以同时承担能力实现，此时使用默认返回值即可；包装第三方客户端或业务 Bean 时应返回被包装的
	 * Core SPI 实例。注册表只按调用方给出的 Core 接口类型进行校验，不会把厂商类型暴露给 Core。</p>
	 *
	 * @return 与描述符类型一致的能力实例，不能为空
	 */
	default Object getCapability() {
		return this;
	}
}
