package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model;

/**
 * 单个 {@code langchain4j-chat-memory-store} 资源实例的提供商参数。
 *
 * <p>按 {@code type} 只读取对应字段；字段留空（{@code null}）时回退全局配置
 * {@code fw.plus.ai.langchain4j.chat-memory.*}，全局也缺省时用实现内置默认值。</p>
 */
public class ChatMemoryStoreInstanceParams {

	/**
	 * jdbc：消息表名（仅允许字母、数字、下划线）。
	 */
	private String tableName;

	/**
	 * redis：键前缀，实际 key 为 {@code keyPrefix + tenantId + ":" + memoryId}。
	 */
	private String keyPrefix;

	/**
	 * redis：过期时间，支持 Spring 风格时长（如 {@code 30m} / {@code 12h} / {@code 7d}），留空永不过期。
	 */
	private String ttl;

	/**
	 * mongodb：集合名。
	 */
	private String collectionName;

	/**
	 * elasticsearch：索引名。
	 */
	private String indexName;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}

	public String getTtl() {
		return ttl;
	}

	public void setTtl(String ttl) {
		this.ttl = ttl;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
}
