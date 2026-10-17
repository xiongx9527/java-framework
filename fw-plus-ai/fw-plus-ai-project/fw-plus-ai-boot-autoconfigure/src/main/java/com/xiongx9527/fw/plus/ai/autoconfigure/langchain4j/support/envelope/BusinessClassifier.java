package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutputException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 业务分类判定：把「本轮用户消息 + 最近若干条用户消息」判定成某个已注册业务名。
 *
 * <p>判定结果只有两种归宿：命中已注册业务（返回该业务名）或回落兜底 {@value BusinessRegistry#FALLBACK}。
 * **任何异常路径都回落兜底**，不让分类失败影响本轮回复——这是通用入口“任何输入都给结果”的前提。</p>
 *
 * <p>要点：</p>
 * <ul>
 *   <li>分类枚举即注册表的业务名清单，因此分类结果天然只会是已注册业务名或兜底名。</li>
 *   <li>分类输入包含最近 N 条用户消息（{@code fw.plus.ai.langchain4j.assistant.classifier.history-user-messages}，
 *       默认 3，0 = 只看本轮）。上一轮是“帮我查订单 A1”、本轮是“那第二单呢”时，没有历史就无法判断业务。</li>
 *   <li>每次轮次都重新判定，不沿用上一轮结果（{@code business} 只由本轮分类决定）。</li>
 *   <li>判定默认不重试（{@code max-attempts} 默认 1）；调大后会走 {@link StructuredOutput} 的错误回喂重试。</li>
 *   <li>模型调用经 {@link StructuredOutput.ChatFunction} 注入：调用方负责“怎么发这一次请求”（直连模型即可，
 *       因此分类过程**不写会话记忆**）。</li>
 * </ul>
 */
@Slf4j
public final class BusinessClassifier {

	/** 分类结果的唯一字段名（同时也是分类 schema 的必填字段） */
	private static final String FIELD_BUSINESS = "business";

	private final BusinessRegistry registry;

	private final Langchain4jAssistantProperties.Classifier config;

	public BusinessClassifier(BusinessRegistry registry, Langchain4jAssistantProperties.Classifier config) {
		this.registry = registry == null ? new BusinessRegistry() : registry;
		this.config = config == null ? new Langchain4jAssistantProperties().getClassifier() : config;
	}

	/**
	 * 判定本轮业务名。
	 *
	 * @param chat               发一次分类请求的方式（直连模型的 {@code generate} 即可）
	 * @param recentUserMessages 最近的历史用户消息（从旧到新；可长可短，本方法按配置截取最近 N 条）
	 * @param message            本轮用户消息原文
	 * @return 已注册的业务名；开关关闭 / 无已注册业务 / 消息为空 / 调用或解析失败时返回
	 *         {@value BusinessRegistry#FALLBACK}
	 */
	public String classify(StructuredOutput.ChatFunction chat, List<String> recentUserMessages, String message) {
		if (!config.isEnabled() || registry.isEmpty() || chat == null || isBlank(message)) {
			return BusinessRegistry.FALLBACK;
		}
		List<String> names = registry.names();
		try {
			JSONObject value = classifierFor(names).chat(chat, buildInput(names, recentUserMessages, message),
					config.getMaxAttempts());
			BusinessContract contract = registry.resolve(value.getString(FIELD_BUSINESS));
			return contract == null ? BusinessRegistry.FALLBACK : contract.getName();
		} catch (StructuredOutputException e) {
			log.debug("业务分类未通过校验，本轮回落兜底 {}：{}", BusinessRegistry.FALLBACK, e.getMessage());
			return BusinessRegistry.FALLBACK;
		} catch (Throwable t) {
			// 连 Error 也收敛（与 EnvelopeAssembler.ensure 同一约定）：分类阶段是整链路的前置环节，
			// 一旦让它抛出，整链路“任何输入都得到合法信封”的承诺就在装配件介入之前被击穿。
			if (t instanceof Error) {
				log.error("业务分类抛出 Error，本轮回落兜底 {}：{}", BusinessRegistry.FALLBACK, t.getMessage(), t);
			} else {
				log.warn("业务分类调用失败，本轮回落兜底 {}：{}", BusinessRegistry.FALLBACK, t.getMessage());
			}
			return BusinessRegistry.FALLBACK;
		}
	}

	// -------------------------------------------------- 内部

	/** 分类 schema：单字段 + 枚举（枚举值即已注册业务名） */
	private static StructuredOutput classifierFor(List<String> names) {
		JSONObject business = new JSONObject();
		business.put("type", "string");
		business.put("enum", names);

		JSONObject properties = new JSONObject();
		properties.put(FIELD_BUSINESS, business);

		JSONObject schema = new JSONObject();
		schema.put("type", "object");
		schema.put("properties", properties);
		schema.put("required", Collections.singletonList(FIELD_BUSINESS));

		return StructuredOutput.of(schema.toJSONString());
	}

	/** 分类输入：可选业务清单 + 最近 N 条用户消息 + 本轮消息 */
	private String buildInput(List<String> names, List<String> recentUserMessages, String message) {
		StringBuilder text = new StringBuilder();
		text.append("请判断下面这轮用户消息属于哪个业务。\n可选业务（输出时必须原样使用业务名称）：");
		text.append(String.join("、", names));
		text.append('\n');

		List<String> history = tailOf(recentUserMessages);
		if (!history.isEmpty()) {
			text.append("\n最近用户消息（从旧到新）：\n");
			for (int i = 0; i < history.size(); i++) {
				text.append(i + 1).append(". ").append(history.get(i)).append('\n');
			}
		}
		text.append("\n本轮用户消息：\n").append(message);
		return text.toString();
	}

	/** 取最近 N 条（N 来自配置，0 表示不要历史） */
	private List<String> tailOf(List<String> messages) {
		if (messages == null || messages.isEmpty() || config.getHistoryUserMessages() <= 0) {
			return Collections.emptyList();
		}
		List<String> cleaned = new ArrayList<String>();
		for (String message : messages) {
			if (!isBlank(message)) {
				cleaned.add(message);
			}
		}
		int from = Math.max(0, cleaned.size() - config.getHistoryUserMessages());
		return cleaned.subList(from, cleaned.size());
	}

	private static boolean isBlank(String text) {
		return text == null || text.trim().isEmpty();
	}
}
