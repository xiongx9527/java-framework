package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 事实抽取策略注册表：按名字命中内置策略（{@code mem0} / {@code llm} / {@code rule} / {@code hybrid}），
 * 按全限定类名反射构造（照 {@code Summarizers.REGISTRY} 先例）；均失败视为未知名，由
 * {@link #resolveOrThrow} 报错（装配层快速失败）或 {@link #resolve} 安静返回 {@code null}。
 *
 * <p>策略语义：</p>
 * <ul>
 *   <li>{@code mem0}（默认）：真实对话原文提交给独立 mem0 实例，由服务端抽取（{@code infer=true}）；</li>
 *   <li>{@code llm}：本地用（便宜）模型把对话抽取为事实 JSON 数组后逐条直存；</li>
 *   <li>{@code rule}：无外部依赖的正则抽取（偏好 / 姓名 / 联系方式 / 编号），静默跳过未命中；</li>
 *   <li>{@code hybrid}：规则候选 + 模型归并去噪；模型缺失退化为纯 {@code rule}，模型失败回退候选。</li>
 * </ul>
 */
public final class FactExtractors {

	public static final String MEM0 = "mem0";

	public static final String LLM = "llm";

	public static final String RULE = "rule";

	public static final String HYBRID = "hybrid";

	/** 默认策略名 */
	public static final String DEFAULT = MEM0;

	private FactExtractors() {
	}

	/**
	 * 解析抽取策略：先按注册名（内置），再按 FQCN 反射（(FactStore, ChatModel) →
	 * (FactStore) → 无参构造）。解析失败返回 {@code null}。
	 */
	public static FactExtractor resolve(String nameOrClass, FactStore store, ChatModel extractModel) {
		return REGISTRY.resolve(nameOrClass, store, extractModel);
	}

	/** 解析抽取策略；失败抛 {@link IllegalArgumentException}（装配层快速失败用）。 */
	public static FactExtractor resolveOrThrow(String nameOrClass, FactStore store, ChatModel extractModel) {
		FactExtractor resolved = resolve(nameOrClass, store, extractModel);
		if (resolved == null) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.FactExtractors.unknownStrategy", MEM0, LLM, RULE, HYBRID, nameOrClass));
		}
		return resolved;
	}

	/** 策略注册表 */
	public static final class REGISTRY {

		private static final Map<String, Builder> BUILDERS = new ConcurrentHashMap<String, Builder>();

		static {
			register(MEM0, (store, model) -> new Mem0FactExtractor(store));
			register(LLM, (store, model) -> new LlmFactExtractor(store, model));
			register(RULE, (store, model) -> new RuleFactExtractor(store));
			register(HYBRID, (store, model) -> new HybridFactExtractor(store, model));
		}

		private REGISTRY() {
		}

		public static void register(String name, Builder builder) {
			if (name == null || name.trim().isEmpty() || builder == null) {
				return;
			}
			BUILDERS.put(name.trim().toLowerCase(), builder);
		}

		public static boolean isBuiltIn(String name) {
			return name != null && BUILDERS.containsKey(name.trim().toLowerCase());
		}

		/** 内置名优先；否则按 FQCN 反射构造；均失败返回 null */
		public static FactExtractor resolve(String nameOrClass, FactStore store, ChatModel model) {
			if (nameOrClass == null || nameOrClass.trim().isEmpty()) {
				return null;
			}
			String key = nameOrClass.trim();
			Builder builder = BUILDERS.get(key.toLowerCase());
			if (builder != null) {
				return builder.build(store, model);
			}
			return reflect(key, store, model);
		}

		private static FactExtractor reflect(String className, FactStore store, ChatModel model) {
			try {
				Class<?> clazz = Class.forName(className);
				if (!FactExtractor.class.isAssignableFrom(clazz)) {
					return null;
				}
				// 依次尝试 (FactStore, ChatModel)、(FactStore)、无参构造
				try {
					Constructor<?> withBoth = clazz.getDeclaredConstructor(FactStore.class, ChatModel.class);
					withBoth.setAccessible(true);
					return (FactExtractor) withBoth.newInstance(store, model);
				} catch (NoSuchMethodException ignored) {
					// 继续尝试下一构造形态
				}
				try {
					Constructor<?> withStore = clazz.getDeclaredConstructor(FactStore.class);
					withStore.setAccessible(true);
					return (FactExtractor) withStore.newInstance(store);
				} catch (NoSuchMethodException ignored) {
					Constructor<?> noArg = clazz.getDeclaredConstructor();
					noArg.setAccessible(true);
					return (FactExtractor) noArg.newInstance();
				}
			} catch (Exception ignored) {
				return null;
			}
		}
	}

	/** 策略工厂（把存储/模型绑定到策略实例） */
	public interface Builder {

		FactExtractor build(FactStore store, ChatModel model);

	}

	// ------------------------------------------------------------------
	// 内置策略
	// ------------------------------------------------------------------

	/** 服务端抽取：真实对话原文提交给独立 mem0 实例（{@code infer=true}），不落本地直存。 */
	public static final class Mem0FactExtractor implements FactExtractor {

		private final FactStore store;

		public Mem0FactExtractor(FactStore store) {
			if (store == null) {
				throw new IllegalArgumentException(
						I18nMessageResolver.resolveKey("fw.ai.FactExtractors.mem0StoreRequired"));
			}
			this.store = store;
		}

		@Override
		public void extract(String userId, List<ChatMessage> turnMessages) {
			String scoped = FactTexts.normalizedUserId(userId);
			if (scoped == null) {
				return;
			}
			store.addDialogue(turnMessages, scoped);
		}
	}

	/** 本地模型抽取：把对话交给模型抽取为事实 JSON 数组，逐条直存（{@code infer=false}）。 */
	@Slf4j
	public static final class LlmFactExtractor implements FactExtractor {

		private final FactStore store;

		private final ChatModel model;

		public LlmFactExtractor(FactStore store, ChatModel model) {
			if (store == null) {
				throw new IllegalArgumentException(
						I18nMessageResolver.resolveKey("fw.ai.FactExtractors.llmStoreRequired"));
			}
			this.store = store;
			this.model = model;
		}

		@Override
		public void extract(String userId, List<ChatMessage> turnMessages) {
			String scoped = FactTexts.normalizedUserId(userId);
			if (scoped == null) {
				return;
			}
			if (model == null) {
				log.warn("llm 抽取策略未配置 extract-model，跳过本轮");
				return;
			}
			List<String> facts;
			try {
				facts = parseJsonFacts(model.chat(FactTexts.extractPrompt(turnMessages)));
			} catch (Exception e) {
				log.warn("llm 事实抽取调用失败，跳过本轮：{}", e.toString());
				return;
			}
			if (facts == null) {
				log.warn("llm 事实抽取输出不可解析，跳过本轮");
				return;
			}
			if (facts.isEmpty()) {
				return;
			}
			store.addFacts(facts, scoped);
		}
	}

	/** 正则抽取：零外部依赖（偏好 / 姓名 / 联系方式 / 编号），未命中静默跳过。 */
	public static final class RuleFactExtractor implements FactExtractor {

		private static final Pattern[] PATTERNS = {
				Pattern.compile("我(?:不喜欢|讨厌|喜欢|喜爱|偏好|习惯|爱)[^。！？!?，,；;\\n]{1,20}"),
				Pattern.compile("我(?:叫|是)[^。！？!?，,；;\\n]{1,20}"),
				Pattern.compile("1[3-9]\\d{9}"),
				Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
				Pattern.compile("(?:订单号|工单号|订单|工单|编号|单号)[:：是为]{0,2}\\s*[A-Za-z0-9-]{6,32}")
		};

		private final FactStore store;

		public RuleFactExtractor(FactStore store) {
			if (store == null) {
				throw new IllegalArgumentException(
						I18nMessageResolver.resolveKey("fw.ai.FactExtractors.ruleStoreRequired"));
			}
			this.store = store;
		}

		@Override
		public void extract(String userId, List<ChatMessage> turnMessages) {
			String scoped = FactTexts.normalizedUserId(userId);
			if (scoped == null) {
				return;
			}
			List<String> facts = collect(turnMessages);
			if (!facts.isEmpty()) {
				store.addFacts(facts, scoped);
			}
		}

		/** 收集用户消息中的规则命中（去重、保持出现顺序）；仅扫描用户消息。 */
		List<String> collect(List<ChatMessage> turnMessages) {
			Set<String> facts = new LinkedHashSet<String>();
			if (turnMessages != null) {
				for (ChatMessage message : turnMessages) {
					if (!(message instanceof UserMessage)) {
						continue;
					}
					String text = FactTexts.textOf(message);
					if (text == null || text.trim().isEmpty()) {
						continue;
					}
					for (Pattern pattern : PATTERNS) {
						Matcher matcher = pattern.matcher(text);
						while (matcher.find()) {
							String fact = matcher.group().trim();
							if (!fact.isEmpty()) {
								facts.add(fact);
							}
						}
					}
				}
			}
			return new ArrayList<String>(facts);
		}
	}

	/** 混合抽取：规则候选 +（可选）模型归并去噪；模型缺失退化为纯 rule，模型失败回退候选。 */
	@Slf4j
	public static final class HybridFactExtractor implements FactExtractor {

		private final FactStore store;

		private final RuleFactExtractor rule;

		private final ChatModel model;

		public HybridFactExtractor(FactStore store, ChatModel model) {
			if (store == null) {
				throw new IllegalArgumentException(
						I18nMessageResolver.resolveKey("fw.ai.FactExtractors.hybridStoreRequired"));
			}
			this.store = store;
			this.rule = new RuleFactExtractor(store);
			this.model = model;
		}

		@Override
		public void extract(String userId, List<ChatMessage> turnMessages) {
			String scoped = FactTexts.normalizedUserId(userId);
			if (scoped == null) {
				return;
			}
			List<String> candidates = rule.collect(turnMessages);
			if (candidates.isEmpty()) {
				return;
			}
			List<String> facts = candidates;
			if (model != null) {
				List<String> merged = null;
				try {
					merged = parseJsonFacts(model.chat(FactTexts.mergePrompt(candidates)));
				} catch (Exception e) {
					log.warn("hybrid 归并调用失败，回退规则候选：{}", e.toString());
				}
				if (merged != null && !merged.isEmpty()) {
					facts = merged;
				} else if (merged != null) {
					log.debug("hybrid 归并结果为空，回退规则候选");
				}
			}
			store.addFacts(facts, scoped);
		}
	}

	// ------------------------------------------------------------------
	// 共享工具
	// ------------------------------------------------------------------

	/** 提示词与文本解析的共享工具（包内可见）。 */
	static final class FactTexts {

		private FactTexts() {
		}

		/** 显式用户段归一化：空白返回 {@code null}（作用域守卫）。 */
		static String normalizedUserId(String userId) {
			if (userId == null || userId.trim().isEmpty()) {
				return null;
			}
			return userId.trim();
		}

		/** 消息文本：用户单文本 / AI / 系统；其余返回 {@code null}。 */
		static String textOf(ChatMessage message) {
			if (message instanceof UserMessage) {
				UserMessage userMessage = (UserMessage) message;
				return userMessage.hasSingleText() ? userMessage.singleText() : null;
			}
			if (message instanceof AiMessage) {
				return ((AiMessage) message).text();
			}
			if (message instanceof SystemMessage) {
				return ((SystemMessage) message).text();
			}
			return null;
		}

		/** llm 策略提示词：真实对话全文 + 只输出 JSON 字符串数组的约束。 */
		static String extractPrompt(List<ChatMessage> turnMessages) {
			StringBuilder prompt = new StringBuilder();
			prompt.append("你是记忆抽取器。请从下面这轮对话中抽取值得长期记住的稳定事实");
			prompt.append("（用户偏好、姓名、身份、联系方式、编号等），忽略寒暄和一次性内容。\n");
			prompt.append("只输出 JSON 字符串数组，例如 [\"用户喜欢深色主题\"]；没有可抽取内容时输出 []。\n\n");
			appendDialogue(prompt, turnMessages);
			return prompt.toString();
		}

		/** hybrid 策略提示词：候选事实归并 / 去重 / 去噪。 */
		static String mergePrompt(List<String> candidates) {
			StringBuilder prompt = new StringBuilder();
			prompt.append("请把下面候选事实归并、去重、去噪，保留稳定且值得长期记住的事实。\n");
			prompt.append("只输出 JSON 字符串数组，例如 [\"用户喜欢深色主题\"]；若无有效事实输出 []。\n\n");
			for (String candidate : candidates) {
				prompt.append("- ").append(candidate).append('\n');
			}
			return prompt.toString();
		}

		private static void appendDialogue(StringBuilder prompt, List<ChatMessage> turnMessages) {
			if (turnMessages == null) {
				return;
			}
			for (ChatMessage message : turnMessages) {
				String text = textOf(message);
				if (text == null || text.trim().isEmpty()) {
					continue;
				}
				prompt.append(roleLabel(message)).append(": ").append(text.trim()).append('\n');
			}
		}

		private static String roleLabel(ChatMessage message) {
			if (message instanceof AiMessage) {
				return "助手";
			}
			if (message instanceof SystemMessage) {
				return "系统";
			}
			return "用户";
		}
	}

	/**
	 * 容忍型 JSON 字符串数组解析：截取首个 {@code [} 到末个 {@code ]} 之间的内容。
	 *
	 * @return 解析成功返回事实列表（可能为空）；不可解析返回 {@code null}
	 */
	static List<String> parseJsonFacts(String text) {
		if (text == null) {
			return null;
		}
		int start = text.indexOf('[');
		int end = text.lastIndexOf(']');
		if (start < 0 || end <= start) {
			return null;
		}
		JsonElement root;
		try {
			root = JsonParser.parseString(text.substring(start, end + 1));
		} catch (Exception e) {
			return null;
		}
		if (root == null || !root.isJsonArray()) {
			return null;
		}
		List<String> facts = new ArrayList<String>();
		for (JsonElement element : root.getAsJsonArray()) {
			if (element.isJsonPrimitive()) {
				String fact = element.getAsString().trim();
				if (!fact.isEmpty()) {
					facts.add(fact);
				}
			}
		}
		return facts;
	}
}
