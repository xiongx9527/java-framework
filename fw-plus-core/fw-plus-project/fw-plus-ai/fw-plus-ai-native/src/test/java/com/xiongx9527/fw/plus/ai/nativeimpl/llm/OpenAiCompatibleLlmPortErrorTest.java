package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmException;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.nativeimpl.support.FakeHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 OpenAI 兼容端口把「200 + error JSON 帧」当作失败上抛——
 * 限流 / 余额不足 / 无权限等常以这种形态返回，必须触发故障转移而非当成成功空响应。
 */
class OpenAiCompatibleLlmPortErrorTest {

	private FakeHttpServer server;

	@BeforeEach
	void start() throws Exception {
		// 返回 SSE error 帧（限流）
		server = new FakeHttpServer(req -> FakeHttpServer.json(200,
				"data: {\"error\":{\"message\":\"rate limit exceeded\",\"type\":\"rate_limit_error\"}}\n\n",
				Collections.singletonMap("Content-Type", "text/event-stream")));
	}

	@AfterEach
	void stop() throws Exception {
		server.close();
	}

	@Test
	void errorFrameSurfacedAsExceptionOnComplete() {
		OpenAiCompatibleLlmPort port = new OpenAiCompatibleLlmPort(server.url(), "k", "model");
		assertThrows(LlmException.class, () -> port.complete(
				LlmRequest.builder().addMessage(ChatMessage.user("hi")).build()));
	}

	@Test
	void errorFrameSurfacedAsOnErrorOnStream() {
		OpenAiCompatibleLlmPort port = new OpenAiCompatibleLlmPort(server.url(), "k", "model");
		AtomicReference<String> error = new AtomicReference<>();
		port.stream(LlmRequest.builder().addMessage(ChatMessage.user("hi")).build())
				.handler(new LlmStreamHandler() {
					@Override
					public void onToken(String content, String reasoningContent) {
					}

					@Override
					public void onComplete() {
					}

					@Override
					public void onError(String message) {
						error.set(message);
					}
				}).start();
		assertEquals("rate limit exceeded", error.get());
	}
}
