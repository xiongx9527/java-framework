package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring AI（Ollama）控制器契约测试：与 LangChain4j 的 {@code ChatControllerTest} 同构。
 *
 * <p>服务层用 {@link MockitoBean} 替换（不连 Ollama），验证入参校验、sessionId 透传与
 * 兜底、出参结构、SSE 事件协议。为防本机环境变量 AI_DASHSCOPE_API_KEY 干扰，
 * 强制置空 api-key（alibaba 链路不装配，与 CI 环境一致）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.ai.dashscope.api-key=", "spring.ai.dashscope.enabled=false"})
class SpringAiChatControllerTest {

	/** sessionId 缺失/空白时的兜底值，与 LangChain4j {@code AiServices.DEFAULT} 语义一致。 */
	private static final String DEFAULT_SESSION_ID = "default";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WeatherTools weatherTools;

	@MockitoBean(name = "springAiOllamaChatService")
	private SpringAiChatService chatService;

	@Test
	void chatReturnsReplyAndToolCalls() throws Exception {
		when(chatService.chat(anyString(), anyString())).thenReturn("杭州夏天更凉快一些。");

		mockMvc.perform(post("/api/chat/spring-ai")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州和北京哪个更适合夏天出行？\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reply").value("杭州夏天更凉快一些。"))
				.andExpect(jsonPath("$.toolCalls").isNumber());
	}

	@Test
	void chatRejectsEmptyMessage() throws Exception {
		mockMvc.perform(post("/api/chat/spring-ai")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void chatPassesSessionIdFromRequestBody() throws Exception {
		when(chatService.chat("s-1", "杭州天气怎么样")).thenReturn("正在查。");

		mockMvc.perform(post("/api/chat/spring-ai")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州天气怎么样\",\"sessionId\":\"s-1\"}"))
				.andExpect(status().isOk());

		verify(chatService).chat("s-1", "杭州天气怎么样");
	}

	@Test
	void chatFallsBackToDefaultWhenSessionIdAbsent() throws Exception {
		when(chatService.chat(DEFAULT_SESSION_ID, "杭州天气怎么样")).thenReturn("正在查。");

		mockMvc.perform(post("/api/chat/spring-ai")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州天气怎么样\"}"))
				.andExpect(status().isOk());

		verify(chatService).chat(DEFAULT_SESSION_ID, "杭州天气怎么样");
	}

	@Test
	void chatFallsBackToDefaultWhenSessionIdBlank() throws Exception {
		when(chatService.chat(DEFAULT_SESSION_ID, "杭州天气怎么样")).thenReturn("正在查。");

		mockMvc.perform(post("/api/chat/spring-ai")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州天气怎么样\",\"sessionId\":\"   \"}"))
				.andExpect(status().isOk());

		verify(chatService).chat(DEFAULT_SESSION_ID, "杭州天气怎么样");
	}

	@Test
	void streamPassesSessionIdAndEmitsSseEvents() throws Exception {
		when(chatService.streamToSse("s-2", "用一句话介绍杭州"))
				.thenAnswer(invocation -> fakeEmitter("你好，杭州。"));

		MvcResult result = mockMvc.perform(post("/api/chat/spring-ai/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"用一句话介绍杭州\",\"sessionId\":\"s-2\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
				.andReturn();

		verify(chatService).streamToSse("s-2", "用一句话介绍杭州");

		String body = readBody(result);
		assertTrue(body.contains("event:start"), "应含 start 事件: " + body);
		assertTrue(body.contains("event:token"), "应含 token 事件: " + body);
		assertTrue(body.contains("event:done"), "应含 done 事件: " + body);
	}

	@Test
	void streamFallsBackToDefaultWhenSessionIdAbsent() throws Exception {
		when(chatService.streamToSse(eq(DEFAULT_SESSION_ID), anyString()))
				.thenAnswer(invocation -> fakeEmitter("你好。"));

		mockMvc.perform(post("/api/chat/spring-ai/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"用一句话介绍杭州\"}"))
				.andExpect(status().isOk());

		verify(chatService).streamToSse(DEFAULT_SESSION_ID, "用一句话介绍杭州");
	}

	@Test
	void streamRejectsEmptyMessage() {
		org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
				mockMvc.perform(post("/api/chat/spring-ai/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"\"}")));
	}

	@Test
	void weatherToolsReturnsMockWeatherAndCountsCalls() {
		weatherTools.reset();
		String json = weatherTools.queryWeather("北京");
		assertTrue(json.contains("temperatureC"), json);
		assertTrue(json.contains("found\":true"), json);
		org.junit.jupiter.api.Assertions.assertEquals(1, weatherTools.getCallCount());
	}

	/** 构造一个立即推完 start/token/done 的 SseEmitter，模拟服务层完成推流。 */
	private static SseEmitter fakeEmitter(String text) {
		SseEmitter emitter = new SseEmitter(0L);
		try {
			emitter.send(SseEmitter.event().name("start").id("r1").data("r1"));
			emitter.send(SseEmitter.event().name("token").id("r1").data(text));
			emitter.send(SseEmitter.event().name("done").id("r1").data(text));
			emitter.complete();
		} catch (IOException e) {
			emitter.completeWithError(e);
		}
		return emitter;
	}

	/**
	 * SSE 响应体读取：emitter 推流在 perform 返回后异步写入 Mock response，
	 * 轮询等待终态事件（done/error）出现；返回 UTF-8 解码的完整内容。
	 */
	private static String readBody(MvcResult result) throws Exception {
		long deadline = System.currentTimeMillis() + 5000;
		String body = "";
		while (System.currentTimeMillis() < deadline) {
			body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			if (body.contains("event:done") || body.contains("event:error")) {
				break;
			}
			Thread.sleep(20);
		}
		return body;
	}
}
