package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.UntypedAgent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 编排注册中心：编排提供方（nl2sql 流水线、agentic-script 组合、业务代码）把编排句柄
 * 按 key 注册，tool-provider 资源链与脚本按 key 引用。
 */
public class AgenticPipelineRegistry {

	private final Map<String, UntypedAgent> pipelines = new ConcurrentHashMap<>();

	/** 按 key 注册（或替换）编排句柄。 */
	public void register(String key, UntypedAgent pipeline) {
		if (key == null || pipeline == null) {
			return;
		}
		pipelines.put(key, pipeline);
	}

	/** 按 key 注销编排。 */
	public void unregister(String key) {
		if (key != null) {
			pipelines.remove(key);
		}
	}

	/** 按 key 查询编排，不存在时返回 {@code null}。 */
	public UntypedAgent get(String key) {
		return key == null ? null : pipelines.get(key);
	}

	/** 是否存在指定 key 的编排。 */
	public boolean contains(String key) {
		return key != null && pipelines.containsKey(key);
	}

	/** 当前已注册的 key 集合（不可修改视图）。 */
	public Set<String> keys() {
		return Collections.unmodifiableSet(pipelines.keySet());
	}
}
