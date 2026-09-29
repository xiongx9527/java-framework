package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

/**
 * {@code langchain4j-chat-memory-store} 数据源资源定义：每份 JSON 对应一个
 * LangChain4j {@code ChatMemoryStore} 实例，get/update/delete 三个动作委托给三个 magic-api 函数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jChatMemoryStoreInfo extends MagicEntity {

	/**
	 * 实例唯一 key，脚本按 key 取 ChatMemoryStore。
	 */
	private String key;

	/**
	 * 展示名，缺省取 key。
	 */
	private String name;

	/**
	 * 是否启用；停用时销毁实例且不注册。
	 */
	private boolean enabled = true;

	/**
	 * 查询消息的函数名（{@code getMessages(memoryId)} 逻辑实现处）。
	 */
	private String getFunction;

	/**
	 * 保存消息的函数名（{@code updateMessages(memoryId, messages)} 逻辑实现处）。
	 */
	private String updateFunction;

	/**
	 * 删除消息的函数名（{@code deleteMessages(memoryId)} 逻辑实现处）。
	 */
	private String deleteFunction;
}
