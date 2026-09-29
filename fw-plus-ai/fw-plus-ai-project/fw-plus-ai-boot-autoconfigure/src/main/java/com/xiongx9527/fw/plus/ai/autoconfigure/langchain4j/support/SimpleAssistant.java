package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;




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
 * // done 事件带完整回复文本；出错发 error 事件（data 为错误信息）后 completeWithError
 * SseEmitter emitter = assistant.streamToSse("session-1", "讲个故事");
 * }</pre>
 *
 * <p>模型（{@code ChatLanguageModel} / {@code LanguageModel} / {@code StreamingLanguageModel}）
 * 一律经构造方法传入，方法内部直接使用对应字段。直连调用用 generate 系列（无记忆、不经 AiServices）：
 * {@code generate(message)} 直连对话、{@code generateText(prompt)} 文本补全、
 * {@code generateTextToSse(prompt)} 补全 SSE 推流；{@code chat / stream / streamToSse} 走 assistant 记忆对话。</p>
 *
 * <pre>{@code
 * SimpleAssistant simple = new SimpleAssistant(chatModel);   // 或四参构造一次给全
 * simple.generate("你好");                                   // 直连一次性对话（无记忆）
 * simple.generate("你是助手", "你好");                        // 直连 + 系统提示
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

	/** SSE 事件名：出错（data 为错误信息，发送后 completeWithError 结束） */
	public static final String EVENT_ERROR = "error";

	private final Assistant assistant;

	private final ChatLanguageModel chatModel;

	private final LanguageModel languageModel;

	private final StreamingLanguageModel streamingLanguageModel;

	/**
	 * 全量构造：底层 {@link Assistant} 与三类模型都由调用方给定（不需要的可传 null），
	 * 便于把外部已有的模型实例装配进同一个封装对象。
	 */
	public SimpleAssistant(Assistant assistant, ChatLanguageModel chatModel,
						   LanguageModel languageModel, StreamingLanguageModel streamingLanguageModel) {
		this.assistant = assistant;
		this.chatModel = chatModel;
		this.languageModel = languageModel;
		this.streamingLanguageModel = streamingLanguageModel;
	}

	/** 全能来源：{@code AiServices<Assistant>}（chat / stream / 记忆 / 工具 / 检索增强） */
	public SimpleAssistant(Assistant assistant) {
		this(assistant, null, null, null);
	}

	/** 仅同步对话：{@code ChatLanguageModel}，无记忆、无流式 */
	public SimpleAssistant(ChatLanguageModel chatModel) {
		this(null, chatModel, null, null);
	}

	/** 仅文本补全：{@code LanguageModel} */
	public SimpleAssistant(LanguageModel languageModel) {
		this(null, null, languageModel, null);
	}

	/** 仅流式补全：{@code StreamingLanguageModel}，逐段推送 */
	public SimpleAssistant(StreamingLanguageModel streamingLanguageModel) {
		this(null, null, null, streamingLanguageModel);
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
		return requireAssistant().chat(memoryId, context, message);
	}

	/** 流式：memoryId 取 {@value #DEFAULT_MEMORY_ID}，context 为空，返回原始 {@link TokenStream} */
	public TokenStream stream(String message) {
		return stream(DEFAULT_MEMORY_ID, message);
	}

	/** 流式：返回原始 {@link TokenStream}，需要自定义回调时用 */
	public TokenStream stream(String memoryId, String message) {
		return requireAssistant().stream(memoryId, DEFAULT_CONTEXT, message);
	}

	// -------------------------------------------------- 直连对话（generate 系列，无记忆）

	/** 直连对话：用构造传入的 {@code ChatLanguageModel} 发一条用户消息并返回回复文本（无记忆） */
	public String generate(String message) {
		return chatModel().generate(UserMessage.from(message)).content().text();
	}

	/** 直连对话（带系统提示）：第一参数为系统提示 */
	public String generate(String systemPrompt, String message) {
		return chatModel().generate(Arrays.asList(SystemMessage.from(systemPrompt), UserMessage.from(message)))
				.content().text();
	}

	/** 直连对话（自定义消息序列）：用构造传入的 {@code ChatLanguageModel}，脚本里拼好上下文再发一次 */
	public String generate(List<ChatMessage> messages) {
		return chatModel().generate(messages).content().text();
	}

	// -------------------------------------------------- 文本补全（语言模型直连）

	/** 一次性文本补全：用构造传入的 {@code LanguageModel}（非对话模型）直接生成文本 */
	public String generateText(String prompt) {
		return languageModel().generate(prompt).content();
	}

	/**
	 * 流式文本补全：用构造传入的 {@code StreamingLanguageModel} 逐段回调 {@code onToken}，
	 * 阻塞等待完成后返回完整文本。
	 *
	 * @param onToken 每段文本回调，可为 null
	 */
	public String generateText(String prompt, java.util.function.Consumer<String> onToken) {
		StringBuilder full = new StringBuilder();
		java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
		streamingLanguageModel().generate(prompt, new dev.langchain4j.model.StreamingResponseHandler<String>() {
			@Override
			public void onNext(String token) {
				full.append(token);
				if (onToken != null) {
					onToken.accept(token);
				}
			}

			@Override
			public void onComplete(dev.langchain4j.model.output.Response<String> response) {
				done.countDown();
			}

			@Override
			public void onError(Throwable throwable) {
				error.set(throwable);
				done.countDown();
			}
		});
		try {
			done.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("流式文本补全被中断", e);
		}
		if (error.get() != null) {
			throw new IllegalStateException("流式文本补全失败", error.get());
		}
		return full.toString();
	}

	// -------------------------------------------------- 流式文本补全的 SSE 推流

	/**
	 * 流式文本补全的 SSE 推流：调用模型前先发 {@code start} 事件把 requestId 交给前端，
	 * 逐段推 {@code token}；结束时发 {@code done} 事件（data 为完整文本，未产出时 [DONE]）并完成，
	 * 异常自动 completeWithError。SseEmitter 不设超时（0L），由生成速度决定。
	 */
	public SseEmitter generateTextToSse(String prompt) {
		return doGenerateTextToSse(prompt, null, null, null, null);
	}

	/** 同上（start 事件 data 由调用方提供，如自己的聊天记录ID；null 时用自动生成的 requestId） */
	public SseEmitter generateTextToSse(String prompt, Object startData) {
		return doGenerateTextToSse(prompt, startData, null, null, null);
	}

	/** 同上（经 {@code onStarted} 拿到本轮 requestId） */
	public SseEmitter generateTextToSse(String prompt, java.util.function.Consumer<String> onStarted) {
		return doGenerateTextToSse(prompt, null, null, onStarted, null);
	}

	/**
	 * 流式文本补全 SSE 推流（start/done 事件 data 均由调用方提供，均非必须）：
	 * {@code startData} 为 start 事件 data，null 时用自动生成的 requestId；
	 * {@code doneDataMapper} 收到完整回复文本（或 [DONE]），返回值作为 done 事件 data，
	 * null 时用完整回复文本——可在这里顺带保存聊天记录。
	 * 注意：与 {@code (prompt, onStarted)} 重载并存，传 null 时请显式转型避免二义性。
	 */
	public SseEmitter generateTextToSse(String prompt, Object startData,
			java.util.function.Function<String, Object> doneDataMapper) {
		return doGenerateTextToSse(prompt, startData, doneDataMapper, null, null);
	}

	/** 同上，并回调挂钩：onStarted 在 start 事件后回调（requestId），onDone 在 done 事件后回调（完整文本或 [DONE]），均可为 null */
	public SseEmitter generateTextToSse(String prompt,
			java.util.function.Consumer<String> onStarted, java.util.function.Consumer<String> onDone) {
		return doGenerateTextToSse(prompt, null, null, onStarted, onDone);
	}

	private SseEmitter doGenerateTextToSse(String prompt, Object startData,
			java.util.function.Function<String, Object> doneDataMapper,
			java.util.function.Consumer<String> onStarted, java.util.function.Consumer<String> onDone) {
		SseEmitter emitter = new SseEmitter(0L);
		String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
		// 客户端断开/超时后置位：后续 token 不再推送也不再消费。
		// langchain4j 0.35.0 的流式 API 无取消句柄，模型侧只能放弃消费（HTTP 流会自然结束）
		java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
		emitter.onCompletion(() -> cancelled.set(true));
		emitter.onError(t -> cancelled.set(true));
		emitter.onTimeout(() -> cancelled.set(true));
		try {
			// 调用模型前先告知前端「已受理」；start 事件 data 默认为 requestId，调用方提供 startData 时用调用方的
			Object start = startData != null ? startData : requestId;
			emitter.send(SseEmitter.event().name(EVENT_START).id(requestId).data(start));
		} catch (Exception e) {
			emitter.completeWithError(e);
			return emitter;
		} finally {
			// finally 保证调用：即使 start 事件 send 失败（客户端已断开）也通知调用方
			if (onStarted != null) {
				onStarted.accept(requestId);
			}
		}

		streamingLanguageModel().generate(prompt, new dev.langchain4j.model.StreamingResponseHandler<String>() {
			@Override
			public void onNext(String token) {
				if (cancelled.get()) {
					return;
				}
				try {
					emitter.send(SseEmitter.event().name(EVENT_TOKEN).id(requestId).data(token));
				} catch (Exception e) {
					cancelled.set(true);
					emitter.completeWithError(e);
				}
			}

			@Override
			public void onComplete(dev.langchain4j.model.output.Response<String> response) {
				// done 事件带上完整回复文本：前端流式中途丢段时可兜底
				String text = response == null || response.content() == null
						|| response.content().isEmpty()
						? DONE_FALLBACK : response.content();
				try {
					if (!cancelled.get()) {
						Object data = doneDataMapper != null ? doneDataMapper.apply(text) : text;
						emitter.send(SseEmitter.event().name(EVENT_DONE).id(requestId).data(data));
						emitter.complete();
					}
				} catch (Exception e) {
					cancelled.set(true);
					emitter.completeWithError(e);
				} finally {
					// finally 保证调用：即使客户端已断开（cancelled）也用模型完整回复补齐聊天记录
					if (onDone != null) {
						onDone.accept(text);
					}
				}
			}

			@Override
			public void onError(Throwable throwable) {
				try {
					if (!cancelled.get()) {
						// 出错也告知前端：先发 error 事件（data 为错误信息）
						emitter.send(SseEmitter.event().name(EVENT_ERROR).id(requestId)
								.data(throwable.getMessage() == null
										? throwable.getClass().getSimpleName() : throwable.getMessage()));
					}
				} catch (Exception ignored) {
					// 客户端已断开等场景：发送失败不影响按错误结束
				} finally {
					emitter.completeWithError(throwable);
				}
			}
		});
		return emitter;
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
		return doStreamToSse(memoryId, message, null, null, null, null);
	}

	/** 同上（start 事件 data 由调用方提供，如自己的聊天记录ID；null 时用自动生成的 requestId） */
	public SseEmitter streamToSse(String memoryId, String message, Object startData) {
		return doStreamToSse(memoryId, message, startData, null, null, null);
	}

	/** 同上（经 {@code onStarted} 拿到本轮 requestId） */
	public SseEmitter streamToSse(String memoryId, String message, Consumer<String> onStarted) {
		return doStreamToSse(memoryId, message, null, null, onStarted, null);
	}

	/**
	 * SSE 推流（start/done 事件 data 均由调用方提供，均非必须）：
	 * {@code startData} 为 start 事件 data，null 时用自动生成的 requestId；
	 * {@code doneDataMapper} 收到完整回复文本（或 [DONE]），返回值作为 done 事件 data，
	 * null 时用完整回复文本——可在这里顺带保存聊天记录。
	 * 注意：与 {@code (memoryId, message, onStarted)} 重载并存，传 null 时请显式转型避免二义性。
	 */
	public SseEmitter streamToSse(String memoryId, String message,
			Object startData, java.util.function.Function<String, Object> doneDataMapper) {
		return doStreamToSse(memoryId, message, startData, doneDataMapper, null, null);
	}

	/**
	 * SSE 推流（挂钩形态）：onStarted 在 start 事件后回调（入参为本轮 requestId），
	 * onDone 在 done 事件后回调（入参为完整回复文本或 [DONE]，finally 保证调用，可用于保存聊天记录）；
	 * 均可为 null。start/done 事件 data 用默认值（requestId / 完整回复文本）。
	 */
	public SseEmitter streamToSse(String memoryId, String message,
			Consumer<String> onStarted, Consumer<String> onDone) {
		return doStreamToSse(memoryId, message, null, null, onStarted, onDone);
	}

	private SseEmitter doStreamToSse(String memoryId, String message, Object startData,
			java.util.function.Function<String, Object> doneDataMapper,
			Consumer<String> onStarted, Consumer<String> onDone) {
		SseEmitter emitter = new SseEmitter(0L);
		String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
		// 客户端断开/超时后置位：后续 token 不再推送也不再消费。
		// langchain4j 0.35.0 的流式 API 无取消句柄，模型侧只能放弃消费（HTTP 流会自然结束）
		java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
		emitter.onCompletion(() -> cancelled.set(true));
		emitter.onError(t -> cancelled.set(true));
		emitter.onTimeout(() -> cancelled.set(true));
		try {
			// 调用模型前先告知前端「已受理」；start 事件 data 默认为 requestId，调用方提供 startData 时用调用方的
			Object start = startData != null ? startData : requestId;
			emitter.send(SseEmitter.event().name(EVENT_START).id(requestId).data(start));
		} catch (Exception e) {
			emitter.completeWithError(e);
			return emitter;
		} finally {
			// finally 保证调用：即使 start 事件 send 失败（客户端已断开）也通知调用方
			if (onStarted != null) {
				onStarted.accept(requestId);
			}
		}

		stream(memoryId, message)
				.onNext(token -> {
					if (cancelled.get()) {
						return;
					}
					try {
						emitter.send(SseEmitter.event().name(EVENT_TOKEN).id(requestId).data(token));
					} catch (Exception e) {
						cancelled.set(true);
						emitter.completeWithError(e);
					}
				})
				.onComplete(response -> {
					// done 事件带上完整回复文本：前端流式中途丢段时可兜底
					String text = doneData(response);
					try {
						if (!cancelled.get()) {
							Object data = doneDataMapper != null ? doneDataMapper.apply(text) : text;
							emitter.send(SseEmitter.event().name(EVENT_DONE).id(requestId).data(data));
							emitter.complete();
						}
					} catch (Exception e) {
						cancelled.set(true);
						emitter.completeWithError(e);
					} finally {
						// finally 保证调用：即使客户端已断开（cancelled）也用模型完整回复补齐聊天记录
						if (onDone != null) {
							onDone.accept(text);
						}
					}
				})
				.onError(throwable -> {
					try {
						if (!cancelled.get()) {
							// 出错也告知前端：先发 error 事件（data 为错误信息）
							emitter.send(SseEmitter.event().name(EVENT_ERROR).id(requestId)
									.data(throwable.getMessage() == null
											? throwable.getClass().getSimpleName() : throwable.getMessage()));
						}
					} catch (Exception ignored) {
						// 客户端已断开等场景：发送失败不影响按错误结束
					} finally {
						emitter.completeWithError(throwable);
					}
				})
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

	// -------------------------------------------------- 内部取用：模型经构造方法传入

	private Assistant requireAssistant() {
		if (assistant == null) {
			throw new IllegalStateException("未配置 Assistant：请用含 Assistant 参数的构造方法构建");
		}
		return assistant;
	}

	private ChatLanguageModel chatModel() {
		if (chatModel == null) {
			throw new IllegalStateException("未配置 ChatLanguageModel：请用构造方法传入");
		}
		return chatModel;
	}

	private LanguageModel languageModel() {
		if (languageModel == null) {
			throw new IllegalStateException("未配置 LanguageModel：请用构造方法传入");
		}
		return languageModel;
	}

	private StreamingLanguageModel streamingLanguageModel() {
		if (streamingLanguageModel == null) {
			throw new IllegalStateException("未配置 StreamingLanguageModel：请用构造方法传入");
		}
		return streamingLanguageModel;
	}
}
