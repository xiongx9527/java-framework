package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.logging;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpLoggingHandlerTest {

	@Test
	void shouldSetPerSessionLoggingLevel() {
		McpLogService logService = new McpLogService();
		McpLoggingHandler handler = new McpLoggingHandler(logService);
		McpRequestContext context = new McpRequestContext();
		context.setSessionId("session-1");

		handler.setLevel(context, Collections.singletonMap("level", "debug"));

		assertThat(logService.getLevel("session-1")).isEqualTo("debug");
	}

	@Test
	void shouldBuildLogNotificationWhenLevelAllows() {
		McpLogService logService = new McpLogService();
		logService.setLevel("session-1", "info");

		Map<String, Object> notification = logService.message("session-1", "warning", "hello");

		assertThat(notification.get("method")).isEqualTo("notifications/message");
	}

	@Test
	void shouldSuppressLogNotificationBelowSessionLevel() {
		McpLogService logService = new McpLogService();
		logService.setLevel("session-1", "error");

		assertThat(logService.message("session-1", "debug", "hidden")).isNull();
	}
}
