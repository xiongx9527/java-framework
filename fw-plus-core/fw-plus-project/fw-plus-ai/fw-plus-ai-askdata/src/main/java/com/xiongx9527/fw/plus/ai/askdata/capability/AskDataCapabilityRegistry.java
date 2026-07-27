package com.xiongx9527.fw.plus.ai.askdata.capability;

import java.util.List;

/**
 * AskData 能力注册和确定性选择契约。
 *
 * <p>注册表只保存厂商无关的能力描述符，Core 不暴露也不访问模型 client、向量库 client
 * 或其他厂商 SDK。Provider 在注册时协商 Core 版本范围，并在集群模式下校验集群兼容性。</p>
 */
public interface AskDataCapabilityRegistry {

	/**
	 * 注册一个能力提供方。
	 *
	 * <p>能力 key 在当前注册表中必须唯一；Core 版本必须落入描述符声明的闭区间，
	 * 集群模式仅接受声明为集群兼容的能力。版本按数字点分段协商，末尾
	 * {@code -SNAPSHOT} 与相同数字段的发布版本等价。</p>
	 *
	 * @param provider 提供能力描述符的 Provider，不能为空
	 * @throws IllegalArgumentException key 重复、版本不兼容或集群不兼容时抛出
	 */
	void register(AskDataCapabilityProvider provider);

	/**
	 * 按 key 获取已注册能力。
	 *
	 * @param key 能力标识
	 * @return 对应的不可变描述符
	 * @throws IllegalArgumentException key 未注册时抛出，异常诊断包含可用 key
	 */
	AskDataCapabilityDescriptor get(String key);

	/**
	 * 列出指定类别的已注册能力。
	 *
	 * <p>返回结果不可修改，并按优先级降序、key 升序稳定排序，以保证各节点选择结果一致。</p>
	 *
	 * @param type 待查询的能力类别，不能为空
	 * @return 不可修改的能力描述符列表
	 * @throws IllegalArgumentException type 为空时抛出
	 */
	List<AskDataCapabilityDescriptor> list(AskDataCapabilityType type);
}
