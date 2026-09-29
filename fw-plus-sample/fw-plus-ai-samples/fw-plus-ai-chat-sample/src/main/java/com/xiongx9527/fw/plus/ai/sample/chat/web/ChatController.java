package com.xiongx9527.fw.plus.ai.sample.chat.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.Assistant;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对话入口：同步 + SSE 流式。
 *
 * <p>{@code sessionId} 会经 {@code Assistant} 上的 {@code @MemoryId} 参数到达
 * {@code AiServices.systemMessageProvider}，系统提示因此可以按会话定制；留空则兜底为
 * {@code default}（不能传 null：LangChain4j 对 {@code @MemoryId} 收到 null 会直接抛异常）。</p>
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

	/** sessionId 缺失/空白时的兜底值，与 LangChain4j {@code AiServices.DEFAULT} 语义一致。 */
	private static final String DEFAULT_SESSION_ID = "default";

	private final Assistant assistant;
	private final WeatherTools weatherTools;

	public ChatController(Assistant assistant, WeatherTools weatherTools) {
		this.assistant = assistant;
		this.weatherTools = weatherTools;
	}

	/**
	 * 同步对话。
	 *
	 * <p>{@code toolCalls} 回报本次请求中天气工具被调用的次数，用于直观验证
	 * "模型自主决定是否调用工具"。</p>
	 */
	@PostMapping
	public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
		String message = request == null ? null : request.getMessage();
		if (message == null || message.trim().isEmpty()) {
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("error", "message 不能为空");
			return ResponseEntity.badRequest().body(error);
		}

		int before = weatherTools.getCallCount();
		String memoryId = resolveSessionId(request.getSessionId());
		String reply = assistant.chat(memoryId, "", message);
		int toolCalls = weatherTools.getCallCount() - before;

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("reply", reply);
		body.put("toolCalls", toolCalls);
		return ResponseEntity.ok(body);
	}

	/**
	 * 流式对话：SSE 逐 token 推送，最后发送 {@code done} 事件。
	 *
	 * <p>请求体与 {@link #chat} 一致（{@code {"message":"...","sessionId":"可选"}}）；
	 * 用 POST 是为了和同步接口同一套入参、不受 URL 长度与编码限制。</p>
	 */
	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@RequestBody ChatRequest request) {
		String message = request == null ? null : request.getMessage();
		if (message == null || message.trim().isEmpty()) {
			throw new IllegalArgumentException("message 不能为空");
		}

		// 0L 表示不设超时，由模型生成速度决定
		SseEmitter emitter = new SseEmitter(0L);
		assistant.stream(resolveSessionId(request.getSessionId()), "", message)
				.onNext(token -> {
					try {
						emitter.send(SseEmitter.event().name("token").data(token));
					} catch (Exception e) {
						emitter.completeWithError(e);
					}
				})
				.onComplete(response -> {
					try {
						emitter.send(SseEmitter.event().name("done").data("[DONE]"));
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
	 * sessionId 兜底：null 或纯空白一律降级为 {@value #DEFAULT_SESSION_ID}。
	 *
	 * <p>LangChain4j 0.35.0 的 {@code DefaultAiServices.findMemoryId} 对 {@code @MemoryId}
	 * 参数收到 null 会抛 IllegalArgumentException，这里挡在 Controller 层，避免客户端
	 * 漏传 sessionId 就让整条对话链路 500。</p>
	 */
	private static String resolveSessionId(String sessionId) {
		return sessionId == null || sessionId.trim().isEmpty() ? DEFAULT_SESSION_ID : sessionId;
	}

	/** 请求体：{@code {"message":"...","sessionId":"可选"}}。 */
	public static class ChatRequest {
		private String message;
		private String sessionId;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getSessionId() {
			return sessionId;
		}

		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}
	}
}
