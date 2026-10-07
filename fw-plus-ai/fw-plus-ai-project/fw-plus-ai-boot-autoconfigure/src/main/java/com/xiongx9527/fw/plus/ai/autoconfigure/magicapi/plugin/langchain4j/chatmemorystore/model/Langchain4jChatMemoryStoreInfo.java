package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

/**
 * {@code langchain4j-chat-memory-store} 数据源资源定义：每份 JSON 对应一个
 * LangChain4j {@code ChatMemoryStore} 实例，由 {@code type} 指定的内置提供商实现存取。
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
	 * 提供商类型：in-memory（内存）/ jdbc / redis / mongodb / elasticsearch，默认内存。
	 */
	private String type = ChatMemoryStoreProperties.TYPE_IN_MEMORY;

	/**
	 * 提供商数据源实例 Key（除内存外可选）：填写则使用 magic-api 同名命名数据源实例
	 * （如 {@code db['key']} / {@code redis['key']} / mongo / elasticsearch 数据源）；
	 * 留空则回退 Spring 容器默认客户端 Bean。
	 */
	private String datasourceKey;

	/**
	 * 提供商参数（按 type 只读取对应字段，留空回退全局配置）：jdbc 传 tableName、
	 * redis 传 keyPrefix/ttl、mongodb 传 collectionName、elasticsearch 传 indexName。
	 */
	private ChatMemoryStoreInstanceParams params;
}
