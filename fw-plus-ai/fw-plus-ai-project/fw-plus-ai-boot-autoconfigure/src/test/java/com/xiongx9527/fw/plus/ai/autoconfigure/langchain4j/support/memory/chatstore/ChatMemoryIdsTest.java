package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * memId 四段契约 {@code {tenantId}:{appId}:{agentId}:{userId}} 的构建与解析。
 */
class ChatMemoryIdsTest {

	@Test
	void buildFourSegments() {
		assertThat(ChatMemoryIds.build("t1", "crm", "bot", "u1")).isEqualTo("t1:crm:bot:u1");
	}

	@Test
	void buildThreeSegmentsDefaultsTenantToZero() {
		assertThat(ChatMemoryIds.build("crm", "bot", "u1")).isEqualTo("0:crm:bot:u1");
		assertThat(ChatMemoryIds.DEFAULT_TENANT_ID).isEqualTo("0");
	}

	@Test
	void buildRejectsEmptySegment() {
		assertThatThrownBy(() -> ChatMemoryIds.build("", "crm", "bot", "u1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("tenantId");
		assertThatThrownBy(() -> ChatMemoryIds.build("t1", "crm", "bot", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("userId");
	}

	@Test
	void buildRejectsSeparatorInSegment() {
		assertThatThrownBy(() -> ChatMemoryIds.build("t1", "crm", "bot", "u:1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("分隔符");
	}

	@Test
	void parseAllSegments() {
		String memoryId = "t1:crm:bot:u1";
		assertThat(ChatMemoryIds.parseTenantId(memoryId, "x")).isEqualTo("t1");
		assertThat(ChatMemoryIds.parseAppId(memoryId, "x")).isEqualTo("crm");
		assertThat(ChatMemoryIds.parseAgentId(memoryId, "x")).isEqualTo("bot");
		assertThat(ChatMemoryIds.parseUserId(memoryId, "x")).isEqualTo("u1");
	}

	@Test
	void shortMemoryIdFallsBackToDefaults() {
		// 段数不足 4（含旧格式 {bizType}:{conversationId}）：各维度整体回退
		String legacy = "order:conv";
		assertThat(ChatMemoryIds.parseTenantId(legacy, "x")).isEqualTo("x");
		assertThat(ChatMemoryIds.parseAppId(legacy, "cfg")).isEqualTo("cfg");
		assertThat(ChatMemoryIds.parseAgentId(legacy, ChatMemoryIds.DEFAULT_AGENT_ID)).isEqualTo("default");
		assertThat(ChatMemoryIds.parseUserId(legacy, ChatMemoryIds.DEFAULT_USER_ID)).isEqualTo("default");
		assertThat(ChatMemoryIds.parseTenantId(null, null)).isNull();
	}

	@Test
	void emptySegmentFallsBackToDefault() {
		String memoryId = "t1::bot:u1";
		assertThat(ChatMemoryIds.parseAppId(memoryId, "cfg")).isEqualTo("cfg");
		assertThat(ChatMemoryIds.parseTenantId(memoryId, "x")).isEqualTo("t1");
		assertThat(ChatMemoryIds.parseUserId(memoryId, "x")).isEqualTo("u1");
	}
}
