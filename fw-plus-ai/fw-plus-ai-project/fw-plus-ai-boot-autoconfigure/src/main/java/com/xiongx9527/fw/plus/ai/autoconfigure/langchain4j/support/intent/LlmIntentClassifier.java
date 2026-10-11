package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * L2 大模型分类器：用结构化输出（JSON）做意图判定，处理规则与向量都覆盖不到的长尾/模糊表达。
 *
 * <p>调用方通过 {@link IntentLlm} 注入模型（与具体模型抽象解耦）。本类负责：</p>
 * <ul>
 *   <li>按枚举常量生成候选意图清单（可附 {@code descriptions} 语义说明）与输出格式约束；</li>
 *   <li>宽容解析模型返回的 JSON —— 支持被 Markdown 代码块包裹、前后有解释文字等噪声；</li>
 *   <li>名称大小写不敏感匹配枚举；无法识别或置信度低于阈值即返回
 *       {@link IntentResult#none(IntentSource)}（不抛错，交级联兜底）。</li>
 * </ul>
 *
 * <p>槽位仅做「存在性」解析，不在组件内做业务校验，避免把业务规则内嵌到通用层。</p>
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * // 1. 用任意模型适配 IntentLlm（与具体版本接口解耦）
 * IntentLlm llm = (systemPrompt, userPrompt) -> chatModel
 *         .generate(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt))
 *         .content().text();
 *
 * // 2. 建议为每个意图补充语义说明，否则模型只能凭枚举名猜测
 * Map<MyIntent, String> descriptions = new HashMap<MyIntent, String>();
 * descriptions.put(MyIntent.LOGISTICS, "查询订单/包裹的物流状态与进度");
 * descriptions.put(MyIntent.REFUND, "申请退款、退货、催促退款到账");
 *
 * IntentClassifier<MyIntent> classifier =
 *         IntentClassifiers.llm(llm, MyIntent.class, descriptions, 0.7d);
 *
 * IntentResult<MyIntent> result = classifier.classify(IntentContext.of("钱什么时候退回来"));
 * // 命中时 intent 来自模型 JSON，slots 为解析出的槽位
 * }</pre>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class LlmIntentClassifier<I extends Enum<I>> implements IntentClassifier<I> {

	private static final Logger logger = LoggerFactory.getLogger(LlmIntentClassifier.class);

	private static final Pattern INTENT_PATTERN = Pattern.compile("\"intent\"\\s*:\\s*\"([^\"]*)\"");

	private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("\"confidence\"\\s*:\\s*(-?[0-9]*\\.?[0-9]+)");

	private static final Pattern SLOTS_PATTERN = Pattern.compile("\"slots\"\\s*:\\s*\\{([^}]*)\\}");

	private static final Pattern SLOT_ENTRY_PATTERN =
			Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([^,}\\s]+))");

	private final IntentLlm llm;

	private final Class<I> intentType;

	private final Map<I, String> descriptions;

	private final double confidenceThreshold;

	public LlmIntentClassifier(IntentLlm llm, Class<I> intentType, Map<I, String> descriptions,
			double confidenceThreshold) {
		if (llm == null) {
			throw new IllegalArgumentException("llm 不能为 null");
		}
		if (intentType == null) {
			throw new IllegalArgumentException("intentType 不能为 null");
		}
		this.llm = llm;
		this.intentType = intentType;
		this.descriptions = descriptions == null
				? Collections.<I, String>emptyMap()
				: new LinkedHashMap<I, String>(descriptions);
		this.confidenceThreshold = confidenceThreshold;
	}

	@Override
	public IntentResult<I> classify(IntentContext context) {
		String text = context == null ? null : context.normalizedText();
		if (text == null || text.isEmpty()) {
			return IntentResult.none(IntentSource.LLM);
		}

		String raw;
		try {
			raw = llm.complete(buildSystemPrompt(), text);
		} catch (RuntimeException e) {
			logger.warn("意图 LLM 判定调用失败，降级为未匹配", e);
			return IntentResult.none(IntentSource.LLM);
		}
		if (raw == null || raw.trim().isEmpty()) {
			return IntentResult.none(IntentSource.LLM);
		}

		I intent = resolveIntent(raw);
		if (intent == null) {
			return IntentResult.none(IntentSource.LLM);
		}
		double confidence = parseConfidence(raw);
		if (confidence < confidenceThreshold) {
			logger.debug("意图 LLM 判定 {} 置信度 {} 低于阈值 {}，视为未匹配", intent, confidence, confidenceThreshold);
			return IntentResult.none(IntentSource.LLM);
		}
		return IntentResult.of(intent, confidence, parseSlots(raw), IntentSource.LLM);
	}

	private String buildSystemPrompt() {
		StringBuilder prompt = new StringBuilder();
		prompt.append("你是一个意图分类器。请从下列意图中选出与用户输入最匹配的一个，");
		prompt.append("只输出一个 JSON 对象，禁止输出任何解释或其他内容。\n");
		prompt.append("JSON 格式：{\"intent\":\"意图名\",\"confidence\":0到1之间的小数,\"slots\":{\"槽位名\":\"槽位值\"}}\n");
		prompt.append("若无法匹配任何意图，输出 {\"intent\":\"NONE\",\"confidence\":0}。\n");
		prompt.append("只能使用下列意图名，不得杜撰：\n");
		for (I candidate : intentType.getEnumConstants()) {
			prompt.append("- ").append(candidate.name());
			String description = descriptions.get(candidate);
			if (description != null && !description.trim().isEmpty()) {
				prompt.append("：").append(description.trim());
			}
			prompt.append('\n');
		}
		return prompt.toString();
	}

	private I resolveIntent(String raw) {
		Matcher matcher = INTENT_PATTERN.matcher(raw);
		if (!matcher.find()) {
			return null;
		}
		String name = matcher.group(1).trim();
		if (name.isEmpty() || "NONE".equalsIgnoreCase(name) || "UNKNOWN".equalsIgnoreCase(name)) {
			return null;
		}
		for (I candidate : intentType.getEnumConstants()) {
			if (candidate.name().equals(name)) {
				return candidate;
			}
		}
		for (I candidate : intentType.getEnumConstants()) {
			if (candidate.name().equalsIgnoreCase(name)) {
				return candidate;
			}
		}
		return null;
	}

	private double parseConfidence(String raw) {
		Matcher matcher = CONFIDENCE_PATTERN.matcher(raw);
		if (!matcher.find()) {
			return 1.0d;
		}
		try {
			double value = Double.parseDouble(matcher.group(1));
			if (value < 0.0d) {
				return 0.0d;
			}
			return Math.min(value, 1.0d);
		} catch (NumberFormatException e) {
			return 1.0d;
		}
	}

	private Map<String, String> parseSlots(String raw) {
		Matcher slotsMatcher = SLOTS_PATTERN.matcher(raw);
		if (!slotsMatcher.find()) {
			return Collections.emptyMap();
		}
		Map<String, String> slots = new LinkedHashMap<String, String>();
		Matcher entry = SLOT_ENTRY_PATTERN.matcher(slotsMatcher.group(1));
		while (entry.find()) {
			String key = entry.group(1);
			if (key == null || key.isEmpty()) {
				continue;
			}
			String value = entry.group(2) != null ? entry.group(2) : entry.group(3);
			slots.put(key, value == null ? "" : value);
		}
		return slots;
	}

	public double confidenceThreshold() {
		return confidenceThreshold;
	}
}
