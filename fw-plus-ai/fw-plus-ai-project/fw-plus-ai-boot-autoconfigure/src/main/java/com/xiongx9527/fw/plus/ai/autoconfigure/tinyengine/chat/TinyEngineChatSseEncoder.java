package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将内部流式分片编码为 Robot 可消费的 OpenAI 兼容 SSE。
 */
public final class TinyEngineChatSseEncoder {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private TinyEngineChatSseEncoder() {
	}

	public static String encode(List<TinyEngineChatStreamChunk> chunks) {
		StringBuilder body = new StringBuilder();
		boolean completed = false;
		boolean contentWritten = false;
		if (chunks != null) {
			for (TinyEngineChatStreamChunk chunk : chunks) {
				if (chunk == null) {
					continue;
				}
				if (chunk.getContent() != null && chunk.getContent().length() > 0
						&& (!chunk.isCompleted() || !contentWritten)) {
					appendFrame(body, chunk.getContent(), null);
					contentWritten = true;
				}
				if (chunk.isCompleted() && !completed) {
					appendFrame(body, null, "stop");
					completed = true;
				}
			}
		}
		if (!completed) {
			appendFrame(body, null, "stop");
		}
		body.append("data: [DONE]\n\n");
		return body.toString();
	}

	public static byte[] encodeBytes(List<TinyEngineChatStreamChunk> chunks) {
		return encode(chunks).getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] encodeJson(Object value) {
		try {
			return OBJECT_MAPPER.writeValueAsBytes(value);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to encode TinyEngine AI JSON response", ex);
		}
	}

	public static Map<String, Object> chatCompletion(TinyEngineChatResponse response, String modelName) {
		Map<String, Object> message = new LinkedHashMap<String, Object>();
		message.put("role", "assistant");
		message.put("content", response == null ? null : response.getContent());

		Map<String, Object> choice = new LinkedHashMap<String, Object>();
		choice.put("index", Integer.valueOf(0));
		choice.put("message", message);
		choice.put("finish_reason", "stop");

		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("object", "chat.completion");
		payload.put("model", modelName);
		payload.put("choices", new ArrayList<Map<String, Object>>(Collections.singletonList(choice)));
		return payload;
	}

	private static void appendFrame(StringBuilder body, String content, String finishReason) {
		Map<String, Object> delta = new LinkedHashMap<String, Object>();
		if (content != null) {
			delta.put("content", content);
		}
		Map<String, Object> choice = new LinkedHashMap<String, Object>();
		choice.put("index", Integer.valueOf(0));
		choice.put("delta", delta);
		choice.put("finish_reason", finishReason);
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("choices", new ArrayList<Map<String, Object>>(Collections.singletonList(choice)));
		try {
			body.append("data: ").append(OBJECT_MAPPER.writeValueAsString(payload)).append("\n\n");
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to encode TinyEngine AI SSE response", ex);
		}
	}
}
