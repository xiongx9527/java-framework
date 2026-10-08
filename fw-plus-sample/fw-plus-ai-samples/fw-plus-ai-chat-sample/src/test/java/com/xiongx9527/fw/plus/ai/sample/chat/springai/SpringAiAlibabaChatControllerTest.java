package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 未配置 DashScope api-key 时的 alibaba 链路契约：应用照常启动，
 * 同步接口返回明确的 503 JSON，流式接口经 SSE error 事件告知原因。
 *
 * <p>强制置空 {@code spring.ai.dashscope.api-key}，保证本机环境变量
 * {@code AI_DASHSCOPE_API_KEY} 不影响测试结论（与 CI 无 key 环境一致）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.ai.dashscope.api-key=", "spring.ai.dashscope.enabled=false"})
class SpringAiAlibabaChatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void chatReturns503WithExplicitErrorWhenNotConfigured() throws Exception {
		mockMvc.perform(post("/api/chat/alibaba")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州天气怎么样\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error").value(containsString("AI_DASHSCOPE_API_KEY")));
	}

	@Test
	void chatRejectsEmptyMessageBeforeConfigCheck() throws Exception {
		// 入参校验优先于配置检查：空消息一律 400
		mockMvc.perform(post("/api/chat/alibaba")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void streamEmitsStartAndErrorEventsWhenNotConfigured() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/chat/alibaba/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"杭州天气怎么样\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
				.andReturn();

		String body = readBody(result);
		assertTrue(body.contains("event:error"), "未配置时流式应发 error 事件: " + body);
		assertTrue(body.contains("AI_DASHSCOPE_API_KEY"), "error 事件应指明缺失的环境变量: " + body);
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
