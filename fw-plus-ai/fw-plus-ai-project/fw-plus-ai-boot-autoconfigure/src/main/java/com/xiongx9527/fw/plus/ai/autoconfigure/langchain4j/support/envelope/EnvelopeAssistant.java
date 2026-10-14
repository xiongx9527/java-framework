package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import lombok.extern.slf4j.Slf4j;

/**
 * 非流式信封入口：一轮对话进来（会话标识 + 用户消息），一个 {@link ChatEnvelope} 出去。
 *
 * <p>与流式通道共用同一个 {@link EnvelopeRuntime}，因此两条通道的信封形态、业务判定与历史语义完全一致：
 * 结构化轮次非流式生成（可按契约重试）、非结构化轮次走既有 AiServices 通道（带记忆/检索/工具）产出正文。</p>
 *
 * <p>入口只接收 {@code memoryId} 与 {@code message}，<b>不接受业务入参</b>——本轮业务只由分类决定（规格明文）。</p>
 *
 * <p><b>永不抛</b>：无论模型异常、校验失败还是既有通道报错，本轮都以 {@code ok=false} 的失败信封收场
 * （{@link Error} 会以 error 级别留痕）。这是与非流式既有入口 {@code SimpleAssistant.chat(...)} 的差异：
 * 既然调用方拿的是“可编程判断的结果”，就不该再让它去接异常。</p>
 */
@Slf4j
public final class EnvelopeAssistant {

	private final EnvelopeRuntime runtime;

	private final ChatCaller chatCaller;

	/**
	 * 非结构化轮次的调用方式：走既有对话通道（AiServices，带记忆/检索/工具），返回模型原文。
	 *
	 * <p>它是本入口与既有通道之间唯一的缝：Spring 场景传 {@code assistant::chat}，
	 * 测试或自定义场景可传任意实现。</p>
	 */
	@FunctionalInterface
	public interface ChatCaller {

		String chat(String memoryId, String message);
	}

	/**
	 * @param runtime    信封编排件
	 * @param chatCaller 非结构化轮次的调用方式
	 */
	public EnvelopeAssistant(EnvelopeRuntime runtime, ChatCaller chatCaller) {
		if (runtime == null) {
			throw new IllegalArgumentException("envelopeRuntime 不能为 null");
		}
		if (chatCaller == null) {
			throw new IllegalArgumentException("chatCaller 不能为 null：非结构化轮次需要既有通道产出正文");
		}
		this.runtime = runtime;
		this.chatCaller = chatCaller;
	}

	/**
	 * 便利构造：非结构化轮次走既有 {@link SimpleAssistant} 通道。
	 *
	 * <p>注意：要让两条通道的历史连续，{@code assistant} 与 {@code runtime} 必须共用同一个
	 * {@code ChatMemoryStore} 与同一个 {@code memoryId}，否则结构化轮次写回的历史闲聊轮次看不到。</p>
	 */
	public EnvelopeAssistant(EnvelopeRuntime runtime, SimpleAssistant assistant) {
		this(runtime, assistant == null ? null : assistant::chat);
	}

	/**
	 * 跑一轮对话并返回信封。
	 *
	 * @param memoryId 会话标识
	 * @param message  本轮用户消息原文
	 * @return 恰好 5 键的合法信封（成功 {@code ok=true}，任何失败 {@code ok=false}）
	 */
	public ChatEnvelope chat(String memoryId, String message) {
		EnvelopeRuntime.Round round = runtime.decide(memoryId, message);
		try {
			if (round.isStructured()) {
				// 结构化：直连模型生成 + 校验 + 回喂重试（重试期间不写历史）
				return runtime.structuredRound(round, memoryId, message);
			}
			// 非结构化：既有通道产出正文后收敛（记忆由既有通道写，本层不重复写）
			return runtime.chatEnvelope(round, chatCaller.chat(memoryId, message));
		} catch (Throwable t) {
			if (t instanceof Error) {
				log.error("非流式轮次抛出 Error，本轮以失败信封结束：{}", t.getMessage(), t);
			} else {
				log.warn("非流式轮次调用既有通道失败，本轮以失败信封结束：{}", t.getMessage());
			}
			return ChatEnvelope.failure(round.getBusiness(), ChatEnvelope.DEFAULT_FAILURE_TEXT, round.getVersion());
		}
	}
}
