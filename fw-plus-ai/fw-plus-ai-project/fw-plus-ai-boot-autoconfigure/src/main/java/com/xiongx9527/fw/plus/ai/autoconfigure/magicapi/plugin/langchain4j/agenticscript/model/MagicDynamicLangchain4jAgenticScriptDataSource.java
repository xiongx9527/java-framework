package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code langchain4j-agentic-script} 数据源的实例持有者：按 {@code key} 持有多个
 * {@link MagicScriptAgent} 脚本节点/工具实例。资源变更即重建（热更新）。
 */
public class MagicDynamicLangchain4jAgenticScriptDataSource {

	private static final Logger logger = LoggerFactory.getLogger(MagicDynamicLangchain4jAgenticScriptDataSource.class);

	private final Map<String, MagicScriptAgent> agents = new ConcurrentHashMap<>();

	/** 按资源定义创建（或重建）一个脚本节点实例。 */
	public boolean create(Langchain4jAgenticScriptInfo info) {
		String key = info.getKey();
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		try {
			destroy(key);
			agents.put(key, new MagicScriptAgent(info, info.getScript()));
			logger.info("注册 LangChain4j Agentic Script: {} -> type={}", key, info.getType());
			return true;
		} catch (Throwable e) {
			logger.warn("构建 LangChain4j Agentic Script 失败: key={}", key, e);
			return false;
		}
	}

	/** 按 key 注销实例。 */
	public void destroy(String key) {
		if (key != null) {
			agents.remove(key);
		}
	}

	/** 按 key 取脚本节点，不存在时返回 {@code null}。 */
	public MagicScriptAgent get(String key) {
		return key == null ? null : agents.get(key);
	}

	/** 当前已配置的 key 集合（不可修改视图）。 */
	public Set<String> keys() {
		return Collections.unmodifiableSet(agents.keySet());
	}
}
