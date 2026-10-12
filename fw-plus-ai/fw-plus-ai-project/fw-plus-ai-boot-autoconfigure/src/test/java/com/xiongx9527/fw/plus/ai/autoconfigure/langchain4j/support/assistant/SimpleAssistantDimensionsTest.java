package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * memId 维度入口 {@code dimensions(...)}：四段契约
 * {@code {tenantId}:{appId}:{agentId}:{userId}}，实例内固定（历史记忆跨会话连续），
 * 未设置维度时维持 {@link SimpleAssistant#DEFAULT_MEMORY_ID}。
 */
class SimpleAssistantDimensionsTest {

	private final Assistant assistant = mock(Assistant.class);

	private final SimpleAssistant simple = new SimpleAssistant(assistant);

	/** 底层 Assistant 签名是多模态 contents 形态：单条文本包装成 TextContent 列表参与 stub/verify */
	private static List<Content> contents(String text) {
		return Collections.singletonList(TextContent.from(text));
	}

	@Test
	void fourDimensionsComposeMemoryId() {
		assertThat(simple.dimensions("t1", "crm", "bot", "u1").memoryId()).isEqualTo("t1:crm:bot:u1");
	}

	@Test
	void threeDimensionsDefaultTenantToZero() {
		assertThat(simple.dimensions("crm", "bot", "u1").memoryId()).isEqualTo("0:crm:bot:u1");
	}

	@Test
	void memoryIdIsStableAcrossCalls() {
		simple.dimensions("t1", "crm", "bot", "u1");
		String first = simple.memoryId();
		assertThat(simple.memoryId()).isEqualTo(first);
		// 重复设置维度后 memId 不变：历史记忆可稳定加载
		simple.dimensions("t1", "crm", "bot", "u1");
		assertThat(simple.memoryId()).isEqualTo(first);
	}

	@Test
	void unconfiguredKeepsDefaultMemoryId() {
		assertThat(simple.memoryId()).isEqualTo(SimpleAssistant.DEFAULT_MEMORY_ID);
	}

	@Test
	void chatSingleArgUsesComposedMemoryId() {
		simple.dimensions("crm", "bot", "u1");
		when(assistant.chat("0:crm:bot:u1", "", contents("你好"))).thenReturn("回复");

		assertThat(simple.chat("你好")).isEqualTo("回复");

		verify(assistant).chat("0:crm:bot:u1", "", contents("你好"));
	}

	@Test
	void chatSingleArgWithoutDimensionsKeepsDefault() {
		when(assistant.chat("default", "", contents("你好"))).thenReturn("回复");

		assertThat(simple.chat("你好")).isEqualTo("回复");

		verify(assistant).chat("default", "", contents("你好"));
	}

	@Test
	void explicitMemoryIdStillWins() {
		simple.dimensions("crm", "bot", "u1");
		when(assistant.chat("session-9", "", contents("你好"))).thenReturn("回复");

		assertThat(simple.chat("session-9", "你好")).isEqualTo("回复");

		verify(assistant).chat("session-9", "", contents("你好"));
	}

	@Test
	void invalidDimensionRejected() {
		assertThatThrownBy(() -> simple.dimensions("", "crm", "bot"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("appId");
		assertThatThrownBy(() -> simple.dimensions("crm", "bot", "u:1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("分隔符");
	}
}
