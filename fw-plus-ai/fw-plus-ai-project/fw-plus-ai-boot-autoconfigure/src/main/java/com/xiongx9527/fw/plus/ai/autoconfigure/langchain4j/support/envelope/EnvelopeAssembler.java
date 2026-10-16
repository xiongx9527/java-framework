package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutputException;
import lombok.extern.slf4j.Slf4j;

/**
 * 信封收敛件：把任意输入收敛成合法 {@link ChatEnvelope}，是“成功失败同形”不变量唯一的落地点。
 *
 * <p>两个通道（流式 {@code done} 帧、非流式返回）都经本类产出信封。{@link #ensure} 是唯一闸门：
 * 无论上游给的是合法 JSON、夹带解释文字的 JSON、被截断的 JSON、纯文本、空回复还是异常对象，
 * 它都返回一个恰好 5 键的合法信封，且**永不抛出**（连 {@link Error} 也被收敛，见
 * {@link #ensure(String, String, RoundMode, Object)}）。</p>
 *
 * <p>两种轮次形态（{@link RoundMode}）：</p>
 * <ul>
 *   <li>{@code STRUCTURED}：宽容解析模型原文，解析出 JSON 对象则产出结构化成功信封
 *       （{@code result} 为对象、{@code text} 为空串）；解析不出（截断、纯文本、顶层是数组）一律降级为失败信封。
 *       {@code result} 顶层恒为对象——数组请让业务契约包一层字段。</li>
 *   <li>{@code CHAT}：模型原文进 {@code text}（不改写），{@code result} 为 {@code null}；
 *       空回复与 {@code [DONE]} 降级为失败信封（不得被当成闲聊成功）。</li>
 * </ul>
 *
 * <p>失败信封只带兜底话术（{@link ChatEnvelope#DEFAULT_FAILURE_TEXT}），**不**把异常信息、校验错误
 * 或模型原文透给调用方；需要排查时由上层记日志。</p>
 *
 * <p><b>职责边界</b>：本类只做**形态**收敛，不做**契约**校验。什么算合法的业务结果由
 * {@link BusinessContract#structuredOutput()} 的 schema + 规则决定；入口负责把「解析/校验失败」
 * 交给 {@link #failure(String, String)}，把「校验通过的对象」交给
 * {@code structured(business, version, value)}。因此本类对任何 {@link JSONObject} 都会产出成功信封。</p>
 */
@Slf4j
public final class EnvelopeAssembler {

	/** 轮次形态：决定同一份原始输入该按结构化解析还是按纯文本收敛 */
	public enum RoundMode {

		/** 结构化轮次：期望模型给出 JSON 对象 */
		STRUCTURED,

		/** 非结构化（闲聊/长文本）轮次：模型原文即结果 */
		CHAT
	}

	/**
	 * 唯一闸门：任何输入都收敛为合法信封，永不抛。
	 *
	 * @param business 本轮命中的业务名（由分类决定）
	 * @param version  业务契约版本（{@code null}/空白时回落 {@link ChatEnvelope#DEFAULT_VERSION}）
	 * @param mode     轮次形态
	 * @param raw      原始输入：{@code String}（模型原文）、{@link JSONObject}（已解析结果）、
	 *                 {@link Throwable}（上游异常）或 {@code null}
	 * @return 恰好 5 键的合法信封；失败路径为 {@code ok=false}、{@code result=null}
	 */
	public ChatEnvelope ensure(String business, String version, RoundMode mode, Object raw) {
		try {
			if (raw == null) {
				return failure(business, version);
			}
			if (raw instanceof ChatEnvelope) {
				// 已经是信封：幂等返回（避免上层重复收敛）
				return (ChatEnvelope) raw;
			}
			if (mode == null || raw instanceof Throwable) {
				// 形态未声明 / 上游异常：一律失败信封（不猜成结构化）
				return failure(business, version);
			}
			return mode == RoundMode.CHAT
					? convergeChat(business, version, raw)
					: convergeStructured(business, version, raw);
		} catch (Throwable t) {
			// 收敛件是最后一道防线：宁可给兜底失败信封，也不能把异常漏给调用方
			if (t instanceof Error) {
				// Error 一并收敛，但必须留痕，否则线上只剩“莫名的失败信封”
				log.error("信封收敛遇到 {}，已降级为失败信封", t.getClass().getName(), t);
			}
			return failureSafely(business, version);
		}
	}

	/** 结构化轮次的便捷入口：等价于 {@code ensure(business, version, STRUCTURED, raw)} */
	public ChatEnvelope structured(String business, String version, Object raw) {
		return ensure(business, version, RoundMode.STRUCTURED, raw);
	}

	/** 闲聊轮次的便捷入口：等价于 {@code ensure(business, version, CHAT, raw)} */
	public ChatEnvelope chat(String business, String version, Object raw) {
		return ensure(business, version, RoundMode.CHAT, raw);
	}

	/** 失败信封：{@code ok=false}、{@code result=null}、{@code text} 为兜底话术 */
	public ChatEnvelope failure(String business, String version) {
		return ChatEnvelope.failure(business, ChatEnvelope.DEFAULT_FAILURE_TEXT, version);
	}

	// -------------------------------------------------- 内部

	private ChatEnvelope convergeChat(String business, String version, Object raw) {
		if (!(raw instanceof String)) {
			// 闲聊轮次的输入只能是模型原文；其它类型说明上游用错了通道
			return failure(business, version);
		}
		String text = (String) raw;
		return isBlank(text) || isDoneMarker(text)
				? failure(business, version)
				: ChatEnvelope.chat(business, text, version);
	}

	private ChatEnvelope convergeStructured(String business, String version, Object raw) {
		if (raw instanceof JSONObject) {
			return ChatEnvelope.structured(business, (JSONObject) raw, version);
		}
		if (!(raw instanceof String)) {
			// 顶层不是对象（数组/数字/布尔/其它对象）一律按失败处理
			return failure(business, version);
		}
		String text = (String) raw;
		if (isBlank(text) || isDoneMarker(text)) {
			return failure(business, version);
		}
		try {
			JSONObject parsed = StructuredOutput.parseReply(text);
			return parsed == null ? failure(business, version)
					: ChatEnvelope.structured(business, parsed, version);
		} catch (StructuredOutputException e) {
			// 截断 / 纯文本 / 顶层数组：都是「本轮没有可用结构」
			return failure(business, version);
		}
	}

	/** 兜底失败信封，且自身绝不抛（连版本号异常也吞掉） */
	private ChatEnvelope failureSafely(String business, String version) {
		try {
			return failure(business, version);
		} catch (Throwable ignored) {
			return ChatEnvelope.failure(null, ChatEnvelope.DEFAULT_FAILURE_TEXT);
		}
	}

	/** 空回复：{@code null} 或全空白 */
	private static boolean isBlank(String text) {
		return text == null || text.trim().isEmpty();
	}

	/** 模型空回复时既有流式路径给出的兜底文本，不能当成本轮成功 */
	private static boolean isDoneMarker(String text) {
		return SimpleAssistant.DONE_FALLBACK.equals(text.trim());
	}
}
