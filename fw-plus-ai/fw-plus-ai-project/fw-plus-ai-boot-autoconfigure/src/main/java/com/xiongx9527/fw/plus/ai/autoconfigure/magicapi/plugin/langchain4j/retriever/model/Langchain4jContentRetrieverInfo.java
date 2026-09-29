package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.Map;

/**
 * {@code langchain4j-content-retriever} 数据源资源定义：每份 JSON 对应一个
 * LangChain4j {@code ContentRetriever} 实例，具体实现类由 provider 短名决定。
 *
 * <p>连接参数、被引用的 datasource key、阈值等差异参数一律放 {@code options}
 * （各 provider 的键集合不同），与七类模型资源保持一致。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jContentRetrieverInfo extends MagicEntity {

	/** datasource type，同时是资源目录名 */
	public static final String TYPE = "langchain4j-content-retriever";

	/** 实例唯一 key，脚本按 key 取 ContentRetriever */
	private String key;

	/** 展示名，缺省取 key */
	private String name;

	/** provider 短名：embedding-store / neo4j / azure-ai-search / web-search / filtering */
	private String provider;

	/** false：销毁实例且不注册（下线而非删除） */
	private boolean enabled = true;

	/** 差异参数对象：不同 provider 的键集合不同 */
	private Map<String, Object> options;
}
