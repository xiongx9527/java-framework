package com.xiongx9527.fw.plus.ai.sample.chat.web;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.Assistant;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对话入口：同步 + SSE 流式。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

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
		String reply = assistant.chat(message);
		int toolCalls = weatherTools.getCallCount() - before;

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("reply", reply);
		body.put("toolCalls", toolCalls);
		return ResponseEntity.ok(body);
	}

	/**
	 * 流式对话：SSE 逐 token 推送，最后发送 {@code done} 事件。
	 */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@RequestParam("message") String message) {
		if (message == null || message.trim().isEmpty()) {
			throw new IllegalArgumentException("message 不能为空");
		}

		// 0L 表示不设超时，由模型生成速度决定
		SseEmitter emitter = new SseEmitter(0L);
		assistant.stream(message)
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

	/** 请求体：{@code {"message":"..."}}。 */
	public static class ChatRequest {
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
