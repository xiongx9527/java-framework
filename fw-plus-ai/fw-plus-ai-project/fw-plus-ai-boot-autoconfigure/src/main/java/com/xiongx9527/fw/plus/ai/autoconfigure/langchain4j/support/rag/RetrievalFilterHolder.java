package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag;

import com.alibaba.fastjson2.JSONObject;

/**
 * RAG 检索过滤条件持有器（ThreadLocal）：调用方在对话/检索前设置元数据过滤条件
 * （Dify metadata_condition 格式），同线程内执行的 ContentRetriever（knowledge 类型）
 * 会读取并叠加到该次检索；用完即清。
 *
 * <p>典型用法（SimpleAssistant 场景，租户 id 等系统级条件由调用方传入，而非用户消息）：</p>
 * <pre>{@code
 * JSONObject condition = JSON.parseObject(
 *     "{\"logical_operator\":\"and\",\"conditions\":[{\"name\":\"tenant_id\",\"comparison_operator\":\"is\",\"value\":\"t01\"}]}");
 * RetrievalFilterHolder.set(condition);
 * try {
 *     String answer = simpleAssistant.chat(memoryId, message);
 * } finally {
 *     RetrievalFilterHolder.clear();
 * }
 * }</pre>
 *
 * <p>限制：仅同线程同步调用生效（AiServices 的同步 chat/retrieve 满足）；
 * 流式 TokenStream 的检索可能发生在回调线程，流式场景请显式检索后走直连生成。</p>
 */
public final class RetrievalFilterHolder {

	private static final ThreadLocal<JSONObject> CONDITIONS = new ThreadLocal<>();

	private RetrievalFilterHolder() {
	}

	public static void set(JSONObject metadataCondition) {
		CONDITIONS.set(metadataCondition);
	}

	/** 读取当前线程条件（可能为 null）。 */
	public static JSONObject get() {
		return CONDITIONS.get();
	}

	public static void clear() {
		CONDITIONS.remove();
	}
}
