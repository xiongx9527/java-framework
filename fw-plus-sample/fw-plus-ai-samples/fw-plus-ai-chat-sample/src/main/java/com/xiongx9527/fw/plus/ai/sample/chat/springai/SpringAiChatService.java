package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.SystemPromptBuilder;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring AI 链路的对话服务：同步 + 流式，Ollama 与 DashScope 各一个实例
 * （构造时注入各自的 {@link ChatClient}，逻辑完全共用）。
 *
 * <p>与 LangChain4j 链路的能力对齐：</p>
 * <ul>
 *   <li>会话记忆：{@code MessageWindowChatMemory}（窗口 20 条）挂在 ChatClient 的默认 Advisor 上，
 *       按 {@code ChatMemory.CONVERSATION_ID}（= sessionId）隔离；</li>
 *   <li>系统提示：{@link SystemPromptBuilder} 人设 + profile（与 LangChain4j 链路同一份构建器）；</li>
 *   <li>工具调用：{@link WeatherTools} 上的 Spring AI {@code @Tool} 注解，由 ChatClient 自动扫描；</li>
 *   <li>知识库 RAG：由 ChatClient 默认 Advisor 上的 {@code RetrievalAugmentationAdvisor}
 *       （Spring AI 原生 RAG 管线）承担——检索经 {@link RemoteKnowledgeBaseDocumentRetriever}
 *       走远程知识库，结果注入本轮 user message、不写入会话记忆，服务层不拼上下文；</li>
 *   <li>SSE 协议：事件名、id、data 形态与 {@link SimpleAssistant#streamToSse} 一致
 *       （start/token/done/error，全事件 id = requestId）。</li>
 * </ul>
 *
 * <p><b>与 LangChain4j 链路的已知差异</b>：记忆用固定窗口截断（不复刻
 * {@code EphemeralChatMemory} 的"只留上一轮语义"）；SSE 不含 thinking/tool-call/
 * retrieved 增量事件（Spring AI 的工具往返由内部 ToolCallingAdvisor 消化，只透出最终答案流；
 * Ollama thinking 走 metadata，暂不透传）。</p>
 */
public class SpringAiChatService {

	private final ChatClient chatClient;
	private final WeatherTools weatherTools;
	private final SystemPromptBuilder systemPromptBuilder;

	public SpringAiChatService(ChatClient chatClient, WeatherTools weatherTools,
			SystemPromptBuilder systemPromptBuilder) {
		this.chatClient = chatClient;
		this.weatherTools = weatherTools;
		this.systemPromptBuilder = systemPromptBuilder;
	}

	/**
	 * 同步对话：返回完整回复文本。
	 */
	public String chat(String sessionId, String message) {
		return buildRequest(sessionId, message).call().content();
	}

	/**
	 * 流式对话：返回增量文本流（供 {@link #streamToSse} 消费，也便于单测直接断言）。
	 */
	public Flux<String> streamTokens(String sessionId, String message) {
		return buildRequest(sessionId, message).stream().content();
	}

	/**
	 * SSE 推流：调用模型前先发 {@code start}（data = requestId），逐段推 {@code token}，
	 * 结束发 {@code done}（data = 完整回复，中途丢段可兜底），异常发 {@code error} 后
	 * completeWithError；客户端断开时取消上游 Flux。协议基准：
	 * {@link SimpleAssistant#streamToSse(String, String)}。
	 */
	public SseEmitter streamToSse(String sessionId, String message) {
		SseEmitter emitter = new SseEmitter(0L);
		String requestId = UUID.randomUUID().toString().replace("-", "");

		AtomicBoolean cancelled = new AtomicBoolean(false);
		AtomicReference<Disposable> disposableRef = new AtomicReference<>();
		Runnable cancelStreaming = () -> {
			Disposable disposable = disposableRef.get();
			if (disposable != null && !disposable.isDisposed()) {
				disposable.dispose();
			}
		};
		emitter.onCompletion(() -> {
			cancelled.set(true);
			cancelStreaming.run();
		});
		emitter.onError(t -> {
			cancelled.set(true);
			cancelStreaming.run();
		});
		emitter.onTimeout(() -> {
			cancelled.set(true);
			cancelStreaming.run();
		});

		try {
			emitter.send(SseEmitter.event().name(SimpleAssistant.EVENT_START).id(requestId).data(requestId));
		} catch (Exception e) {
			emitter.completeWithError(e);
			return emitter;
		}

		StringBuilder fullText = new StringBuilder();
		Disposable disposable = streamTokens(sessionId, message).subscribe(
				delta -> {
					if (cancelled.get() || delta == null || delta.isEmpty()) {
						return;
					}
					fullText.append(delta);
					try {
						emitter.send(SseEmitter.event()
								.name(SimpleAssistant.EVENT_TOKEN).id(requestId).data(delta));
					} catch (Exception e) {
						// 客户端已断开：停止推送并取消上游
						cancelled.set(true);
						cancelStreaming.run();
					}
				},
				error -> {
					if (cancelled.getAndSet(true)) {
						return;
					}
					String data = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
					try {
						emitter.send(SseEmitter.event()
								.name(SimpleAssistant.EVENT_ERROR).id(requestId).data(data));
					} catch (Exception ignored) {
						// 发送失败不影响按错误结束
					}
					emitter.completeWithError(error);
				},
				() -> {
					if (cancelled.getAndSet(true)) {
						return;
					}
					try {
						emitter.send(SseEmitter.event()
								.name(SimpleAssistant.EVENT_DONE).id(requestId).data(fullText.toString()));
						emitter.complete();
					} catch (Exception e) {
						emitter.completeWithError(e);
					}
				});
		disposableRef.set(disposable);
		// 订阅在 set 之前同步完成（如立即错误）时兜底取消
		if (cancelled.get()) {
			cancelStreaming.run();
		}
		return emitter;
	}

	/**
	 * 组装本轮请求：系统提示（人设 + profile）+ 用户消息 + 天气工具 + 会话记忆参数。
	 * 知识库上下文不在此拼装——由 ChatClient 默认 Advisor 上的
	 * {@code RetrievalAugmentationAdvisor} 在 advisor 链中检索并注入本轮 user message。
	 */
	private ChatClient.ChatClientRequestSpec buildRequest(String sessionId, String message) {
		return chatClient.prompt()
				.system(systemPromptBuilder.build(sessionId))
				.user(message)
				.tools(weatherTools)
				.advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId));
	}
}
