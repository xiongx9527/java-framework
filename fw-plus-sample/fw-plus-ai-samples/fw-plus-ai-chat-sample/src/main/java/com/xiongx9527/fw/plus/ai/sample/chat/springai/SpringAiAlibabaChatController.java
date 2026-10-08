package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import com.xiongx9527.fw.plus.ai.sample.chat.web.ChatController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spring AI Alibaba（DashScope / 通义千问）对话入口：同步 + SSE 流式。
 *
 * <p>入参、出参、SSE 协议与 {@link SpringAiChatController} 完全一致，仅后端模型不同。
 * 未配置 {@code spring.ai.dashscope.api-key} 时服务 Bean 不装配
 * （见 {@link SpringAiAlibabaChatConfig}），应用照常启动，此时：</p>
 * <ul>
 *   <li>同步接口返回 503 + {@code {"error": ...}} 明确说明；</li>
 *   <li>流式接口按既有 SSE 协议先发 {@code start} 再发 {@code error} 事件携带同一段说明
 *       （HTTP 状态保持 200，错误经事件通道传达——与模型出错时的推送方式一致）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/chat/alibaba")
public class SpringAiAlibabaChatController {

	/** sessionId 缺失/空白时的兜底值，与 LangChain4j {@code AiServices.DEFAULT} 语义一致。 */
	private static final String DEFAULT_SESSION_ID = "default";

	/** 未配置 DashScope key 时的错误说明。 */
	private static final String NOT_CONFIGURED_ERROR =
			"未配置 DashScope api-key：请设置环境变量 AI_DASHSCOPE_API_KEY"
					+ "（或 spring.ai.dashscope.api-key）后重启，alibaba 链路才可用";

	private final ObjectProvider<SpringAiChatService> chatServiceProvider;
	private final WeatherTools weatherTools;

	public SpringAiAlibabaChatController(
			@Qualifier("springAiDashScopeChatService") ObjectProvider<SpringAiChatService> chatServiceProvider,
			WeatherTools weatherTools) {
		this.chatServiceProvider = chatServiceProvider;
		this.weatherTools = weatherTools;
	}

	/**
	 * 同步对话；未配置 api-key 时返回 503 与明确错误。
	 *
	 * <p>{@code toolCalls} 回报本次请求中天气工具被调用的次数，与另两套实现语义一致。</p>
	 */
	@PostMapping
	public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatController.ChatRequest request) {
		String message = request == null ? null : request.getMessage();
		if (message == null || message.trim().isEmpty()) {
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("error", "message 不能为空");
			return ResponseEntity.badRequest().body(error);
		}

		SpringAiChatService chatService = chatServiceProvider.getIfAvailable();
		if (chatService == null) {
			Map<String, Object> error = new LinkedHashMap<>();
			error.put("error", NOT_CONFIGURED_ERROR);
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
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
	 * 流式对话；未配置 api-key 时通过 SSE {@code error} 事件携带明确错误说明。
	 *
	 * <p>请求体与 {@link #chat} 一致（{@code {"message":"...","sessionId":"可选"}}）。</p>
	 */
	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@RequestBody ChatController.ChatRequest request) {
		String message = request == null ? null : request.getMessage();
		if (message == null || message.trim().isEmpty()) {
			throw new IllegalArgumentException("message 不能为空");
		}

		SpringAiChatService chatService = chatServiceProvider.getIfAvailable();
		if (chatService == null) {
			return notConfiguredEmitter();
		}

		String sessionId = resolveSessionId(request.getSessionId());
		return chatService.streamToSse(sessionId, message);
	}

	/**
	 * 未配置 api-key 的流式错误推送：start（data = requestId）→ error（data = 说明）→ 结束。
	 * 事件形态与 {@code SimpleAssistant.streamToSse} 的错误路径一致。
	 */
	private static SseEmitter notConfiguredEmitter() {
		SseEmitter emitter = new SseEmitter(0L);
		String requestId = UUID.randomUUID().toString().replace("-", "");
		try {
			emitter.send(SseEmitter.event().name(SimpleAssistant.EVENT_START).id(requestId).data(requestId));
			emitter.send(SseEmitter.event().name(SimpleAssistant.EVENT_ERROR).id(requestId)
					.data(NOT_CONFIGURED_ERROR));
			emitter.complete();
		} catch (Exception e) {
			emitter.completeWithError(e);
		}
		return emitter;
	}

	/**
	 * sessionId 兜底：null 或纯空白一律降级为 {@value #DEFAULT_SESSION_ID}。
	 */
	private static String resolveSessionId(String sessionId) {
		return sessionId == null || sessionId.trim().isEmpty() ? DEFAULT_SESSION_ID : sessionId;
	}
}
