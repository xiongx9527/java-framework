package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Consumer;

/**
 * {@link Assistant} 的易用封装：常用参数给了默认值，省掉样板代码。
 *
 * <p>{@code Assistant} 的三个参数（memoryId / context / message）里，{@code context} 在
 * 不做动态系统提示定制时恒为空串；这里的重载直接把它省掉：</p>
 *
 * <pre>{@code
 * SimpleAssistant assistant = new SimpleAssistant(assistant);
 * assistant.chat("用一句话介绍杭州");                       // memoryId=default，context=""
 * assistant.chat("session-1", "杭州天气怎么样");             // memoryId 自定，context=""
 *
 * // SSE 推流：start 事件先带 requestId 表示已受理，token 事件逐段推送，
 * // done 事件带完整回复文本，异常自动 completeWithError
 * SseEmitter emitter = assistant.streamToSse("session-1", "讲个故事");
 * }</pre>
 */
public class SimpleAssistant {

	/** memoryId 缺省值，与 LangChain4j {@code AiServices.DEFAULT} 语义一致 */
	public static final String DEFAULT_MEMORY_ID = "default";

	/** 上下文参数的缺省值：不做动态系统提示定制时恒为空串 */
	public static final String DEFAULT_CONTEXT = "";

	/** done 事件的兜底数据：模型没有产出文本时使用 */
	public static final String DONE_FALLBACK = "[DONE]";

	/** SSE 事件名：开始（data 为 requestId） */
	public static final String EVENT_START = "start";

	/** SSE 事件名：逐段 token */
	public static final String EVENT_TOKEN = "token";

	/** SSE 事件名：结束（data 为完整回复文本或 {@value #DONE_FALLBACK}） */
	public static final String EVENT_DONE = "done";

	private final Assistant assistant;

	public SimpleAssistant(Assistant assistant) {
		this.assistant = assistant;
	}

	/** 需要底层 {@link Assistant} 类型（如再挂别的 AiServices 能力）时取用 */
	public Assistant toAssistant() {
		return assistant;
	}

	/** 一次性对话：memoryId 取 {@value #DEFAULT_MEMORY_ID}，context 为空 */
	public String chat(String message) {
		return chat(DEFAULT_MEMORY_ID, message);
	}

	/** 会话对话：memoryId 自定（决定会话记忆），context 为空 */
	public String chat(String memoryId, String message) {
		return chat(memoryId, DEFAULT_CONTEXT, message);
	}

	/** 完整形态：透传给底层 {@link Assistant} */
	public String chat(String memoryId, String context, String message) {
		return assistant.chat(memoryId, context, message);
	}

	/** 流式：memoryId 取 {@value #DEFAULT_MEMORY_ID}，context 为空，返回原始 {@link TokenStream} */
	public TokenStream stream(String message) {
		return stream(DEFAULT_MEMORY_ID, message);
	}

	/** 流式：返回原始 {@link TokenStream}，需要自定义回调时用 */
	public TokenStream stream(String memoryId, String message) {
		return assistant.stream(memoryId, DEFAULT_CONTEXT, message);
	}

	/**
	 * SSE 推流（自动生成 requestId）：调用模型前先发 {@code start} 事件把 requestId 交给前端，
	 * 再逐段推 {@code token}；结束时发 {@code done} 事件并完成，异常自动 completeWithError。
	 * 每个事件都带 SSE 标准 id 字段（值为本轮 requestId），前端可用 {@code lastEventId} 取到。
	 * SseEmitter 不设超时（0L），由生成速度决定。
	 */
	public SseEmitter streamToSse(String message) {
		return streamToSse(DEFAULT_MEMORY_ID, message);
	}

	public SseEmitter streamToSse(String memoryId, String message) {
		return streamToSse(memoryId, message, null);
	}

	/**
	 * SSE 推流（调用方可提供 requestId，并经 {@code onStarted} 拿到它）。
	 *
	 * <p>调用模型前先发 {@code start} 事件（data 即 requestId），表示本轮已受理；
	 * 若 {@code emitter.send} 本身失败（如客户端已断开），直接 completeWithError 且不再调用模型。</p>
	 *
	 * @param onStarted start 事件发送成功后回调（入参为本轮 requestId），可为 null
	 */
	public SseEmitter streamToSse(String memoryId, String message, Consumer<String> onStarted) {
		SseEmitter emitter = new SseEmitter(0L);
		String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
		try {
			// 调用模型前先告知前端「已受理」，并下发本轮 requestId
			emitter.send(SseEmitter.event().name(EVENT_START).id(requestId).data(requestId));
			if (onStarted != null) {
				onStarted.accept(requestId);
			}
		} catch (Exception e) {
			emitter.completeWithError(e);
			return emitter;
		}

		stream(memoryId, message)
				.onNext(token -> {
					try {
						emitter.send(SseEmitter.event().name(EVENT_TOKEN).id(requestId).data(token));
					} catch (Exception e) {
						emitter.completeWithError(e);
					}
				})
				.onComplete(response -> {
					try {
						// done 事件带上完整回复文本：前端流式中途丢段时可兜底
						emitter.send(SseEmitter.event().name(EVENT_DONE).id(requestId).data(doneData(response)));
						emitter.complete();
					} catch (Exception e) {
						emitter.completeWithError(e);
					}
				})
				.onError(emitter::completeWithError)
				.start();
		return emitter;
	}

	/**
	 * done 事件的数据：完整回复文本；模型没有产出文本（如纯工具调用轮、response 为空）时
	 * 退化为 {@code [DONE]} 标记。
	 */
	static String doneData(Response<AiMessage> response) {
		if (response == null || response.content() == null
				|| response.content().text() == null || response.content().text().isEmpty()) {
			return DONE_FALLBACK;
		}
		return response.content().text();
	}
}
