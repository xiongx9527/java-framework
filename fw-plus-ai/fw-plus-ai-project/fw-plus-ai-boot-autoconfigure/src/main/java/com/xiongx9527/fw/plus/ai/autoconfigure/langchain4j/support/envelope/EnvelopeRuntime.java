package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 信封编排件：把「一轮对话」变成「{@link ChatEnvelope}」，流式与非流式两条通道共用它。
 *
 * <p>一轮的处理顺序固定：</p>
 *
 * <ol>
 *   <li>{@link #decide} 读会话历史 → 分类本轮业务 → 解析契约 → 定下本轮形态
 *       （{@link Round#isStructured()}：命中契约 <b>且</b> 结构化开关打开才是结构化轮次）；</li>
 *   <li>结构化轮次走 {@link #structuredRound}（非流式生成、可按契约重试、写回历史）；
 *       非结构化轮次走既有 AiServices 通道产出正文，再用 {@link #chatEnvelope} 收敛（不写记忆）。</li>
 * </ol>
 *
 * <p><b>永不抛</b>：{@link #decide}、{@link #structuredRound}、{@link #chatEnvelope} 对任何上游异常
 * （含 {@link Error}，见 {@link EnvelopeAssembler#ensure}）都收敛成兜底结果，不把异常漏给调用方——
 * 这样流式通道在分类与生成阶段都不会掉进 {@code error} 帧，非流式入口也不会抛给脚本。</p>
 *
 * <p><b>记忆</b>：本类自己持有一份 {@link EnvelopeSessionMemory}（与既有 AiServices 通道共用同一个
 * {@link ChatMemoryStore} 与 {@code memoryId}），只做两件事——读快照喂给分类与结构化生成、
 * 在结构化轮次结束时写回「用户原文 + 结果摘要」。非结构化轮次的记忆由既有通道负责写，
 * 因此 {@link #chatEnvelope} <b>不写</b>（否则同一轮会写两遍）。</p>
 *
 * <p><b>重试零写入</b>：结构化轮次的消息上下文在重试循环之外只取一次快照，重试期间不写任何历史，
 * 所以重试不会看到自己上一次的失败输出，也不会把校验错误原文灌进会话记忆。</p>
 */
@Slf4j
public final class EnvelopeRuntime {

	/** 契约未声明系统提示时的默认系统提示 */
	public static final String DEFAULT_SYSTEM_HINT = "你是一个业务助手。按下面的结构要求，从用户消息中提取结果。";

	private final ChatModel chatModel;

	private final BusinessRegistry registry;

	private final Langchain4jAssistantProperties properties;

	private final BusinessRouter router;

	private final EnvelopeSessionMemory memory;

	private final EnvelopeAssembler assembler = new EnvelopeAssembler();

	/**
	 * @param chatModel  直连模型：分类与结构化生成都用它（不经 AiServices、不受工具/检索影响）
	 * @param store      会话记忆存储；必须与既有对话通道用同一个 store，否则两条通道的历史会各走各的
	 * @param registry   业务契约注册表；{@code null} 等价于没有注册任何业务（所有轮次落兜底 chat）
	 * @param properties 助手配置
	 */
	public EnvelopeRuntime(ChatModel chatModel, ChatMemoryStore store, BusinessRegistry registry,
			Langchain4jAssistantProperties properties) {
		if (chatModel == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.EnvelopeRuntime.chatModelRequired"));
		}
		if (store == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.EnvelopeRuntime.chatMemoryStoreRequired"));
		}
		if (properties == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.EnvelopeRuntime.propertiesRequired"));
		}
		this.chatModel = chatModel;
		this.registry = registry == null ? new BusinessRegistry() : registry;
		this.properties = properties;
		this.router = new BusinessRouter(this.registry, properties.getClassifier());
		this.memory = new EnvelopeSessionMemory(store, properties.getSession().getWindowSize());
	}

	// -------------------------------------------------- 第一段：定下本轮业务与形态

	/**
	 * 判定本轮业务与本轮形态（每轮重新判定，不沿用上一轮结果）。
	 *
	 * <p>分类输入 = 本轮用户消息 + 最近 N 条历史用户消息（N 由
	 * {@code fw.plus.ai.langchain4j.assistant.classifier.history-user-messages} 决定，0 = 只看本轮）。
	 * 分类只读历史、绝不写历史。</p>
	 *
	 * @param memoryId 会话标识（同时是记忆的 id）
	 * @param message  本轮用户消息原文
	 * @return 本轮判定结果；分类不可用 / 未命中 / 任何异常一律回落兜底 {@value BusinessRegistry#FALLBACK} 轮次
	 */
	public Round decide(String memoryId, String message) {
		return decide(memoryId, message, null);
	}

	/**
	 * 同上，携带<b>单次调用</b>层的形态覆盖（三层覆盖顺序的最高层：单次调用 &gt; 业务契约 &gt; 全局默认）。
	 *
	 * <p>本层是规格三层覆盖中「单次调用」的唯一产品入口——既有流式入口的签名不可改动，
	 * 因此单次覆盖只在非流式信封入口提供（{@code EnvelopeAssistant.chat(memoryId, message, Boolean)}）。</p>
	 *
	 * @param structuredOverride 本次调用是否强制产出结构化结果：{@code null} = 不覆盖（按业务契约与全局配置）；
	 *                           {@code TRUE}/{@code FALSE} = 本次调用开启/关闭结构化。
	 *                           兜底轮次（未命中任何契约）恒为非结构化——没有契约就没有可校验的 schema，
	 *                           此时 {@code TRUE} 也不产出 result
	 */
	public Round decide(String memoryId, String message, Boolean structuredOverride) {
		String requestId = newRequestId();
		BusinessRouter.RoutedTurn turn = null;
		try {
			turn = router.route(this::sendOnce, historyUserMessages(memoryId), message);
		} catch (Throwable t) {
			// 读历史或分类失败都不得让本轮失败：一律落兜底 chat 轮次
			logFailure("业务分类", t);
		}
		BusinessContract contract = turn == null ? null : turn.getContract();
		String business = turn == null ? BusinessRegistry.FALLBACK : turn.getBusiness();
		boolean structured = contract != null
				&& properties.getStructured().resolveEnabled(structuredOverride, contract.getStructured());
		String version = contract == null ? ChatEnvelope.DEFAULT_VERSION : contract.getVersion();
		return new Round(requestId, business, contract, structured, version);
	}

	// -------------------------------------------------- 第二段之一：结构化轮次

	/**
	 * 结构化轮次（非流式通道）：直连模型生成一次结果，不符合契约就把错误回喂重试，最多
	 * {@code 契约 maxAttempts > 全局 max-attempts} 次。
	 *
	 * <p>消息序列 = 契约系统提示（未声明则 {@link #DEFAULT_SYSTEM_HINT}）+ 会话历史快照 +
	 * 「schema 指令 + 本轮文本」。轮次结束时写回历史：成功写「用户原文 + 结果摘要」，
	 * 失败写「用户原文 + 失败说明」（校验错误原文不进历史）。</p>
	 *
	 * <p>永不抛：模型异常、解析失败、校验超限一律收敛为 {@code ok=false} 失败信封。</p>
	 */
	public ChatEnvelope structuredRound(Round round, String memoryId, String message) {
		return doStructuredRound(round, memoryId, message, maxAttempts(round));
	}

	/**
	 * 结构化轮次（流式通道）：只尝试一次。
	 *
	 * <p>流式通道的模型输出已经逐帧下发，重发请求会让调用方收到重复内容，因此这里不重试；
	 * 一次拿不到合法结果就以 {@code ok=false} 信封收场。</p>
	 */
	public ChatEnvelope structuredRoundOnce(Round round, String memoryId, String message) {
		return doStructuredRound(round, memoryId, message, 1);
	}

	// -------------------------------------------------- 第二段之二：非结构化轮次

	/**
	 * 非结构化轮次：把既有通道产出的模型原文收敛成信封。
	 *
	 * <p>本方法<b>不写记忆</b>——闲聊轮次的记忆由产出这段原文的既有通道写，重复写会让同一轮出现两条记录。</p>
	 */
	public ChatEnvelope chatEnvelope(Round round, String text) {
		String business = round == null ? BusinessRegistry.FALLBACK : round.getBusiness();
		String version = round == null ? ChatEnvelope.DEFAULT_VERSION : round.getVersion();
		return assembler.chat(business, version, text);
	}

	// -------------------------------------------------- 内部：结构化轮次

	private ChatEnvelope doStructuredRound(Round round, String memoryId, String message, int maxAttempts) {
		if (round == null || !round.isStructured()) {
			// 契约调用错误（不是输入问题）：本轮根本没有契约可校验，无法产出有意义的 result
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.EnvelopeRuntime.structuredRoundRequired"));
		}
		BusinessContract contract = round.getContract();
		try {
			// 快照只取一次：重试期间不写入历史，因此每次尝试看到同一份上下文
			List<ChatMessage> prefix = new ArrayList<ChatMessage>();
			prefix.add(SystemMessage.from(systemHint(contract)));
			prefix.addAll(memory.snapshot(memoryId));
			int prefixSize = prefix.size();

			StructuredOutput.ChatFunction send = prompt -> chatModel
					.chat(ChatRequest.builder().messages(withPrompt(prefix, prefixSize, prompt)).build())
					.aiMessage().text();

			JSONObject value = contract.structuredOutput().chat(send, message, maxAttempts);
			ChatEnvelope envelope = assembler.structured(round.getBusiness(), round.getVersion(), value);
			recordTurnQuietly(memoryId, message, envelope);
			return envelope;
		} catch (Throwable t) {
			logFailure("结构化轮次", t);
			ChatEnvelope failure = assembler.failure(round.getBusiness(), round.getVersion());
			recordTurnQuietly(memoryId, message, failure);
			return failure;
		}
	}

	/** 历史快照 + 本轮提示词（提示词每次尝试都重新拼，历史部分复用同一份快照） */
	private static List<ChatMessage> withPrompt(List<ChatMessage> prefix, int prefixSize, String prompt) {
		List<ChatMessage> messages = new ArrayList<ChatMessage>(prefix.subList(0, prefixSize));
		messages.add(UserMessage.from(prompt));
		return messages;
	}

	private int maxAttempts(Round round) {
		if (round == null || round.getContract() == null || round.getContract().getMaxAttempts() == null) {
			return properties.getStructured().getMaxAttempts();
		}
		return round.getContract().getMaxAttempts();
	}

	private static String systemHint(BusinessContract contract) {
		String hint = contract.getSystemHint();
		return hint == null || hint.trim().isEmpty() ? DEFAULT_SYSTEM_HINT : hint.trim();
	}

	// -------------------------------------------------- 内部：分类与历史

	/** 直连发送一次分类请求：单条用户消息（提示词），不经 AiServices、不写会话记忆 */
	private String sendOnce(String prompt) {
		return chatModel.chat(ChatRequest.builder().messages(UserMessage.from(prompt)).build())
				.aiMessage().text();
	}

	/** 该会话的历史用户消息（从旧到新，已按窗口裁剪）；条数如何截取由分类器按配置处理 */
	private List<String> historyUserMessages(String memoryId) {
		List<ChatMessage> snapshot = memory.snapshot(memoryId);
		List<String> texts = new ArrayList<String>();
		for (ChatMessage message : snapshot) {
			if (message instanceof UserMessage) {
				texts.add(((UserMessage) message).singleText());
			}
		}
		return texts;
	}

	private void recordTurnQuietly(String memoryId, String message, ChatEnvelope envelope) {
		try {
			memory.recordTurn(memoryId, message, envelope);
		} catch (Throwable t) {
			// 写历史失败不得推翻本轮已经算出的结果
			logFailure("会话历史写回", t);
		}
	}

	private void logFailure(String what, Throwable t) {
		if (t instanceof Error) {
			log.error("{}抛出 Error，已收敛：{}", what, t.getMessage(), t);
		} else {
			log.warn("{}未成功，已收敛：{}", what, t.getMessage());
		}
	}

	private static String newRequestId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	// -------------------------------------------------- 轮次判定结果

	/**
	 * 本轮判定结果：业务名、命中的契约（兜底轮次为 {@code null}）、本轮形态与本轮版本。
	 *
	 * <p>它只承载判定，不承载生成结果——生成结果一律是 {@link ChatEnvelope}。</p>
	 */
	public static final class Round {

		/** start 帧 data 里本轮请求标识的键名 */
		public static final String KEY_REQUEST_ID = "requestId";

		/** start 帧 data 里本轮业务名的键名 */
		public static final String KEY_BUSINESS = "business";

		/** start 帧 data 里「本轮是否逐字下发」的键名 */
		public static final String KEY_TOKENS = "tokens";

		private final String requestId;

		private final String business;

		private final BusinessContract contract;

		private final boolean structured;

		private final String version;

		public Round(String requestId, String business, BusinessContract contract, boolean structured, String version) {
			this.requestId = requestId;
			this.business = business;
			this.contract = contract;
			this.structured = structured;
			this.version = version;
		}

		/** 本轮请求标识（与事件 id 一致，便于前端把 start 与 done 对上） */
		public String getRequestId() {
			return requestId;
		}

		/** 本轮业务名（由分类决定；兜底轮次为 {@value BusinessRegistry#FALLBACK}） */
		public String getBusiness() {
			return business;
		}

		/** 命中的契约；兜底轮次为 {@code null} */
		public BusinessContract getContract() {
			return contract;
		}

		/** 本轮是否产出结构化结果（命中契约 <b>且</b> 结构化开关打开）；决定是否发 {@code token} 帧 */
		public boolean isStructured() {
			return structured;
		}

		/** 本轮信封 {@code v} 的取值 */
		public String getVersion() {
			return version;
		}

		/**
		 * start 帧 data：{@code {requestId, business, tokens}}。
		 *
		 * <p>分类完成后才发（业务名是分类结果，发早了自己都不知道填什么）。
		 * 开关开启时框架接管 start 帧，调用方传入的 {@code startData} 不再使用。</p>
		 */
		public JSONObject getStartData() {
			JSONObject data = new JSONObject();
			data.put(KEY_REQUEST_ID, requestId);
			data.put(KEY_BUSINESS, business);
			data.put(KEY_TOKENS, !structured);
			return data;
		}

		@Override
		public String toString() {
			return "Round{" + business + ", structured=" + structured + ", v=" + version + "}";
		}
	}
}
