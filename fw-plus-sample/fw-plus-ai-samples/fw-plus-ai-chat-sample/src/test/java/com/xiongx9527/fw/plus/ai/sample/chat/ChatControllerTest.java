package com.xiongx9527.fw.plus.ai.sample.chat;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.Assistant;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 控制器层测试：同步接口与 SSE 流式接口的契约。
 *
 * <p>测试不连 Ollama，用 {@link MockitoBean} 替换 Assistant；未配置知识库 base-url，
 * RAG Bean 不装配，同时覆盖降级路径。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

	/** sessionId 缺失/空白时的兜底值，与 LangChain4j {@code AiServices.DEFAULT} 语义一致。 */
	private static final String DEFAULT_SESSION_ID = "default";

	/** 底层 Assistant 签名是多模态 contents 形态：单条文本包装成 TextContent 列表参与 stub/verify */
	private static List<Content> contents(String text) {
		return Collections.singletonList(TextContent.from(text));
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Assistant assistant;

	@Autowired
	private WeatherTools weatherTools;

	@Test
	void chatReturnsReplyAndToolCalls() throws Exception {
		when(assistant.chat(anyString(), eq(""), anyList())).thenReturn("杭州夏天更凉快一些。");

		mockMvc.perform(post("/api/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州和北京哪个更适合夏天出行？\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reply").value("杭州夏天更凉快一些。"))
				.andExpect(jsonPath("$.toolCalls").isNumber());
	}

	@Test
	void chatRejectsEmptyMessage() throws Exception {
		mockMvc.perform(post("/api/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void chatPassesSessionIdFromRequestBody() throws Exception {
		when(assistant.chat("s-1", "", contents("杭州天气怎么样"))).thenReturn("正在查。");

		mockMvc.perform(post("/api/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州天气怎么样\",\"sessionId\":\"s-1\"}"))
				.andExpect(status().isOk());

		verify(assistant).chat("s-1", "", contents("杭州天气怎么样"));
	}

	@Test
	void chatFallsBackToDefaultWhenSessionIdAbsent() throws Exception {
		mockMvc.perform(post("/api/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州天气怎么样\"}"))
				.andExpect(status().isOk());

		verify(assistant).chat(DEFAULT_SESSION_ID, "", contents("杭州天气怎么样"));
	}

	@Test
	void chatFallsBackToDefaultWhenSessionIdBlank() throws Exception {
		mockMvc.perform(post("/api/chat")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州天气怎么样\",\"sessionId\":\"   \"}"))
				.andExpect(status().isOk());

		verify(assistant).chat(DEFAULT_SESSION_ID, "", contents("杭州天气怎么样"));
	}

	@Test
	void streamPassesSessionIdFromQueryParam() throws Exception {
		when(assistant.stream("s-2", "", contents("用一句话介绍杭州"))).thenAnswer(invocation -> new FakeTokenStream().complete());

		mockMvc.perform(post("/api/chat/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"用一句话介绍杭州\",\"sessionId\":\"s-2\"}"))
				.andExpect(status().isOk());

		verify(assistant).stream("s-2", "", contents("用一句话介绍杭州"));
	}

	@Test
	void streamFallsBackToDefaultWhenSessionIdAbsent() throws Exception {
		when(assistant.stream(DEFAULT_SESSION_ID, "", contents("用一句话介绍杭州")))
				.thenAnswer(invocation -> new FakeTokenStream().complete());

		mockMvc.perform(post("/api/chat/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"用一句话介绍杭州\"}"))
				.andExpect(status().isOk());

		verify(assistant).stream(DEFAULT_SESSION_ID, "", contents("用一句话介绍杭州"));
	}

	@Test
	void streamEmitsTokenAndDoneEvents() throws Exception {
		when(assistant.stream(anyString(), eq(""), anyList())).thenAnswer(invocation -> new FakeTokenStream().complete());

		mockMvc.perform(post("/api/chat/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"用一句话介绍杭州\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
	}

	@Test
	void weatherToolsReturnsMockWeatherAndCountsCalls() {
		weatherTools.reset();
		String json = weatherTools.queryWeather("北京");
		org.junit.jupiter.api.Assertions.assertTrue(json.contains("temperatureC"), json);
		org.junit.jupiter.api.Assertions.assertTrue(json.contains("found\":true"), json);
		org.junit.jupiter.api.Assertions.assertEquals(1, weatherTools.getCallCount());
	}

	/**
	 * 可编程的 TokenStream 替身：在 start() 时依次触发已注册的回调。
	 * 抽象方法必须全部实现：onRetrieved /
	 * onNext / onComplete / onError / ignoreErrors，其余为 default。
	 */
	private static class FakeTokenStream implements dev.langchain4j.service.TokenStream {

		private Consumer<String> partialHandler;
		private Consumer<dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>> completeHandler;
		private boolean completed = false;

		FakeTokenStream complete() {
			completed = true;
			return this;
		}

		@Override
		public dev.langchain4j.service.TokenStream onNext(Consumer<String> handler) {
			partialHandler = handler;
			return this;
		}

		@Override
		public dev.langchain4j.service.TokenStream onRetrieved(
				Consumer<java.util.List<dev.langchain4j.rag.content.Content>> handler) {
			return this;
		}


		@Override
		public dev.langchain4j.service.TokenStream onComplete(
				Consumer<dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage>> handler) {
			completeHandler = handler;
			return this;
		}

		@Override
		public dev.langchain4j.service.TokenStream onError(Consumer<Throwable> handler) {
			return this;
		}

		@Override
		public dev.langchain4j.service.TokenStream ignoreErrors() {
			return this;
		}

		@Override
		public void start() {
			if (partialHandler != null) {
				partialHandler.accept("杭州");
				partialHandler.accept("夏天");
			}
			if (completed && completeHandler != null) {
				completeHandler.accept(null);
			}
		}
	}
}
