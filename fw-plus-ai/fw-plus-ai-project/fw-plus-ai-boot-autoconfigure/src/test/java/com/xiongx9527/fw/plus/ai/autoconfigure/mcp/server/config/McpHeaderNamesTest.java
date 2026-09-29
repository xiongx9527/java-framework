package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpHeaderNamesTest {

	@Test
	void shouldFallBackToKernelDefaultsWhenNothingConfigured() {
		McpHeaderNames names = new McpHeaderNames(new McpProperties.Headers());

		assertThat(names.sessionId()).isEqualTo(McpHeaderNames.DEFAULT_SESSION_ID);
		assertThat(names.protocolVersion()).isEqualTo(McpHeaderNames.DEFAULT_PROTOCOL_VERSION);
		assertThat(names.accept()).isEqualTo(McpHeaderNames.DEFAULT_ACCEPT);
		assertThat(names.origin()).isEqualTo(McpHeaderNames.DEFAULT_ORIGIN);
	}

	@Test
	void shouldUseConfiguredNamesWhenPresent() {
		McpProperties.Headers headers = new McpProperties.Headers();
		headers.setSessionId("X-Session");
		headers.setProtocolVersion("X-Protocol");
		headers.setAccept("X-Accept");
		headers.setOrigin("X-Origin");
		McpHeaderNames names = new McpHeaderNames(headers);

		assertThat(names.sessionId()).isEqualTo("X-Session");
		assertThat(names.protocolVersion()).isEqualTo("X-Protocol");
		assertThat(names.accept()).isEqualTo("X-Accept");
		assertThat(names.origin()).isEqualTo("X-Origin");
	}

	@Test
	void shouldCopyCustomHeaderValueUnderKernelDefaultName() {
		McpProperties.Headers headers = new McpProperties.Headers();
		headers.setSessionId("X-Session");
		McpHeaderNames names = new McpHeaderNames(headers);
		Map<String, String> values = new HashMap<>();
		values.put("X-Session", "abc-123");
		values.put("Accept", "application/json");

		names.applyCustomNames(values);

		assertThat(values).containsEntry(McpHeaderNames.DEFAULT_SESSION_ID, "abc-123");
		assertThat(values).containsEntry("X-Session", "abc-123");
		assertThat(values).containsEntry("Accept", "application/json");
	}

	@Test
	void shouldNotOverwriteDefaultNameWhenCustomHeaderAbsent() {
		McpProperties.Headers headers = new McpProperties.Headers();
		headers.setSessionId("X-Session");
		McpHeaderNames names = new McpHeaderNames(headers);
		Map<String, String> values = new HashMap<>();
		values.put(McpHeaderNames.DEFAULT_SESSION_ID, "kernel-default");

		names.applyCustomNames(values);

		assertThat(values).containsEntry(McpHeaderNames.DEFAULT_SESSION_ID, "kernel-default");
		assertThat(values).doesNotContainKey("X-Session");
	}

	@Test
	void shouldSkipMappingWhenConfiguredNameEqualsDefault() {
		McpProperties.Headers headers = new McpProperties.Headers();
		headers.setSessionId(McpHeaderNames.DEFAULT_SESSION_ID);
		McpHeaderNames names = new McpHeaderNames(headers);
		Map<String, String> values = new HashMap<>();
		values.put(McpHeaderNames.DEFAULT_SESSION_ID, "abc-123");

		names.applyCustomNames(values);

		assertThat(values).hasSize(1);
		assertThat(values).containsEntry(McpHeaderNames.DEFAULT_SESSION_ID, "abc-123");
	}
}
