package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.SimpleAssistant;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.output.Response;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.List;

/**
 * LangChain4j 脚本模块入口：用 {@code langchain4j.assistant()} 快速构建助手
 * （{@code AiServices<Assistant>}：模型 + 记忆 + 系统提示 + 工具 + 检索增强），
 * 只要一次对话则用 {@code langchain4j.chat(...)}。
 *
 * <p>本模块不注入任何东西：模型、记忆、工具、检索器都由脚本从各类型模块取出来后作为参数传入，
 * 例如 {@code langchain4jChatModel['qwen-main']}、{@code langchain4jContentRetriever['kb']}。</p>
 *
 * <pre>{@code
 * import langchain4j;
 * import langchain4jChatModel;
 * import langchain4jContentRetriever;
 *
 * var assistant = langchain4j.assistant()
 *     .chatModel(langchain4jChatModel['qwen-main'])
 *     .streamingModel(langchain4jStreamingChatModel['qwen-stream'])   // 需要流式时才配
 *     .systemMessage('你是助手，回答简洁专业')
 *     .memory(20)                                                    // 进程内记忆，窗口 20 条
 *     .tools(weatherTool)                                            // @Tool 对象
 *     .contentRetriever(langchain4jContentRetriever['kb'])           // 检索增强
 *     .build();
 *
 * var reply = assistant.chat('session-1', '', '杭州天气怎么样');
 * var once = langchain4j.chat(langchain4jChatModel['qwen-main'], '用一句话介绍杭州');
 * }</pre>
 */
@MagicModule("langchain4j")
public class Langchain4jModule {

	/**
	 * 一次性文本补全：用语言模型（非对话模型）直接生成文本。
	 *
	 * @param languageModel 语言模型，如 {@code langchain4jLanguageModel['key']}
	 * @param prompt        提示文本
	 */
	public String complete(dev.langchain4j.model.language.LanguageModel languageModel, String prompt) {
		return languageModel.generate(prompt).content();
	}

	/**
	 * 流式文本补全：逐段回调 {@code onToken}，阻塞等待完成后返回完整文本。
	 *
	 * @param streamingLanguageModel 流式语言模型，如 {@code langchain4jStreamingLanguageModel['key']}
	 * @param prompt                 提示文本
	 * @param onToken                每段文本回调，可为 null
	 */
	public String complete(dev.langchain4j.model.language.StreamingLanguageModel streamingLanguageModel,
			String prompt, java.util.function.Consumer<String> onToken) {
		StringBuilder full = new StringBuilder();
		java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
		java.util.concurrent.atomic.AtomicReference<Throwable> error = new java.util.concurrent.atomic.AtomicReference<>();
		streamingLanguageModel.generate(prompt, new dev.langchain4j.model.StreamingResponseHandler<String>() {
			@Override
			public void onNext(String token) {
				full.append(token);
				if (onToken != null) {
					onToken.accept(token);
				}
			}

			@Override
			public void onComplete(Response<String> response) {
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

	/**
	 * 流式文本补全的 SSE 推流：调用模型前先发 {@code start} 事件把 requestId 交给前端，
	 * 逐段推 {@code token}；结束时发 {@code done} 事件（data 为完整文本，未产出时 [DONE]）并完成，
	 * 异常自动 completeWithError。SseEmitter 不设超时（0L），由生成速度决定。
	 *
	 * @param streamingLanguageModel 流式语言模型，如 {@code langchain4jStreamingLanguageModel['key']}
	 * @param prompt                 提示文本
	 */
	public SseEmitter completeToSse(
			dev.langchain4j.model.language.StreamingLanguageModel streamingLanguageModel, String prompt) {
		return completeToSse(streamingLanguageModel, prompt, null);
	}

	/** 同上（调用方可提供 requestId，并经 {@code onStarted} 拿到它） */
	public SseEmitter completeToSse(
			dev.langchain4j.model.language.StreamingLanguageModel streamingLanguageModel,
			String prompt, java.util.function.Consumer<String> onStarted) {
		SseEmitter emitter =
				new SseEmitter(0L);
		String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
		try {
			// 调用模型前先告知前端「已受理」，并下发本轮 requestId
			emitter.send(SseEmitter.event()
					.name(SimpleAssistant.EVENT_START).id(requestId).data(requestId));
			if (onStarted != null) {
				onStarted.accept(requestId);
			}
		} catch (Exception e) {
			emitter.completeWithError(e);
			return emitter;
		}

		streamingLanguageModel.generate(prompt, new dev.langchain4j.model.StreamingResponseHandler<String>() {
			@Override
			public void onNext(String token) {
				try {
					emitter.send(SseEmitter.event()
							.name(SimpleAssistant.EVENT_TOKEN).id(requestId).data(token));
				} catch (Exception e) {
					emitter.completeWithError(e);
				}
			}

			@Override
			public void onComplete(Response<String> response) {
				try {
					// done 事件带上完整回复文本：前端流式中途丢段时可兜底
					String text = response == null || response.content() == null
							|| response.content().isEmpty()
							? SimpleAssistant.DONE_FALLBACK : response.content();
					emitter.send(SseEmitter.event()
							.name(SimpleAssistant.EVENT_DONE).id(requestId).data(text));
					emitter.complete();
				} catch (Exception e) {
					emitter.completeWithError(e);
				}
			}

			@Override
			public void onError(Throwable throwable) {
				emitter.completeWithError(throwable);
			}
		});
		return emitter;
	}

	/** 快速构建助手：链式配置模型/记忆/系统提示/工具/检索，最后 {@code build()} 得到 {@code Assistant}。 */
	public AssistantBuilder assistant() {
		return new AssistantBuilder();
	}

	/**
	 * 一次性对话：发一条用户消息并返回回复文本（无记忆）。
	 *
	 * @param chatModel 对话模型，如 {@code langchain4jChatModel['qwen-main']}
	 * @param message   用户消息
	 */
	public String chat(ChatLanguageModel chatModel, String message) {
		return chatModel.generate(UserMessage.from(message)).content().text();
	}

	/**
	 * 一次性对话（带系统提示）。
	 *
	 * @param chatModel    对话模型
	 * @param systemPrompt 系统提示文本
	 * @param message      用户消息
	 */
	public String chat(ChatLanguageModel chatModel, String systemPrompt, String message) {
		return chatModel.generate(Arrays.asList(SystemMessage.from(systemPrompt), UserMessage.from(message)))
				.content().text();
	}

	/**
	 * 一次性对话（自定义消息序列）：脚本里拼好上下文再发一次。
	 *
	 * @param chatModel 对话模型
	 * @param messages  完整消息序列（System / User / Ai / Tool 均可）
	 */
	public String chat(ChatLanguageModel chatModel, List<ChatMessage> messages) {
		return chatModel.generate(messages).content().text();
	}
}
