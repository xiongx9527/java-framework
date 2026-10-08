package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import com.xiongx9527.fw.plus.ai.sample.chat.web.ChatController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring AI（Ollama）对话入口：同步 + SSE 流式。
 *
 * <p>与 LangChain4j 的 {@link ChatController} 完全同构：入参 {@code {"message","sessionId"}}、
 * 出参 {@code {reply, toolCalls}}、sessionId 空白兜底 {@code default}、SSE 协议一致，
 * 便于在同一前端上切换三套实现对比。</p>
 */
@RestController
@RequestMapping("/api/chat/spring-ai")
public class SpringAiChatController {

	/** sessionId 缺失/空白时的兜底值，与 LangChain4j {@code AiServices.DEFAULT} 语义一致。 */
	private static final String DEFAULT_SESSION_ID = "default";

	private final SpringAiChatService chatService;
	private final WeatherTools weatherTools;

	public SpringAiChatController(
			@Qualifier("springAiOllamaChatService") SpringAiChatService chatService,
			WeatherTools weatherTools) {
		this.chatService = chatService;
		this.weatherTools = weatherTools;
	}

	/**
	 * 同步对话。
	 *
	 * <p>{@code toolCalls} 回报本次请求中天气工具被调用的次数（与 LangChain4j 链路
	 * 共享同一个计数器），用于直观验证"模型自主决定是否调用工具"。</p>
	 */
	@PostMapping
	public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatController.ChatRequest request) {
		String message = request == null ? null : request.getMessage();
		if (message == null || message.trim().isEmpty()) {
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("error", "message 不能为空");
			return ResponseEntity.badRequest().body(error);
		}

		int before = weatherTools.getCallCount();
		String sessionId = resolveSessionId(request.getSessionId());
		String reply = chatService.chat(sessionId, message);
		int toolCalls = weatherTools.getCallCount() - before;

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("reply", reply);
		body.put("toolCalls", toolCalls);
		return ResponseEntity.ok(body);
	}

	/**
	 * 流式对话：SSE 逐 token 推送，最后发送 {@code done} 事件（data 为完整回复文本）。
	 */
	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@RequestBody ChatController.ChatRequest request) {
		String message = request == null ? null : request.getMessage();
		if (message == null || message.trim().isEmpty()) {
			throw new IllegalArgumentException("message 不能为空");
		}

		String sessionId = resolveSessionId(request.getSessionId());
		return chatService.streamToSse(sessionId, message);
	}

	/**
	 * sessionId 兜底：null 或纯空白一律降级为 {@value #DEFAULT_SESSION_ID}。
	 */
	private static String resolveSessionId(String sessionId) {
		return sessionId == null || sessionId.trim().isEmpty() ? DEFAULT_SESSION_ID : sessionId;
	}
}
