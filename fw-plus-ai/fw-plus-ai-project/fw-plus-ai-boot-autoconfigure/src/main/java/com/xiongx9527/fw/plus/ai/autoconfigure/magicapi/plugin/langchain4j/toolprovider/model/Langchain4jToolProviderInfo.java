package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.Langchain4jToolProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

/**
 * {@code langchain4j-tool-provider} 数据源资源定义：每份 JSON 对应一个独立的
 * LangChain4j {@code ToolProvider}（独立工具范围），脚本用 {@code langchain4jToolProvider['key']} 取用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jToolProviderInfo extends MagicEntity {

	/**
	 * 实例唯一 key，脚本按 key 取 ToolProvider。
	 */
	private String key;

	/**
	 * 展示名，缺省取 key。
	 */
	private String name;

	/**
	 * 是否启用；停用时销毁实例且不注册（下线而非删除）。
	 */
	private boolean enabled = true;

	/**
	 * 该实例的工具范围配置（functions 与 api 两类），缺省全量且不过滤。
	 */
	private Langchain4jToolProperties.Tools tools;
}
