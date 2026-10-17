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
		String effective = simple.effectiveMemoryId();
		assertThat(effective).isEqualTo("0:crm:bot:u1:" + simple.sessionId());
		when(assistant.chat(effective, "", contents("你好"))).thenReturn("回复");

		assertThat(simple.chat("你好")).isEqualTo("回复");

		verify(assistant).chat(effective, "", contents("你好"));
	}

	@Test
	void chatSingleArgUsesSessionizedKeyNotBareMemoryId() {
		simple.dimensions("crm", "bot", "u1");
		// 对话路径必须用 5 段会话键（而非 4 段 memoryId()），否则摘要锚点会跨会话串用
		assertThat(simple.effectiveMemoryId()).isNotEqualTo(simple.memoryId());
		assertThat(simple.effectiveMemoryId()).matches("0:crm:bot:u1:d\\d{8}");
	}

	@Test
	void sessionSwitchIsolatesConversationKey() {
		simple.dimensions("crm", "bot", "u1");
		simple.session("s-1");
		assertThat(simple.effectiveMemoryId()).isEqualTo("0:crm:bot:u1:s-1");
		when(assistant.chat("0:crm:bot:u1:s-1", "", contents("你好"))).thenReturn("第一会话");

		simple.session("s-2");
		assertThat(simple.effectiveMemoryId()).isEqualTo("0:crm:bot:u1:s-2");
		when(assistant.chat("0:crm:bot:u1:s-2", "", contents("你好"))).thenReturn("第二会话");

		// 同一维度下切换会话：记忆键确实不同（会话级隔离）
		simple.session("s-1");
		assertThat(simple.chat("你好")).isEqualTo("第一会话");
		simple.session("s-2");
		assertThat(simple.chat("你好")).isEqualTo("第二会话");
		verify(assistant).chat("0:crm:bot:u1:s-1", "", contents("你好"));
		verify(assistant).chat("0:crm:bot:u1:s-2", "", contents("你好"));
	}

	@Test
	void tokenStreamSingleArgUsesSessionizedKey() {
		simple.dimensions("crm", "bot", "u1");
		simple.session("s-7");
		when(assistant.stream("0:crm:bot:u1:s-7", "", contents("你好")))
				.thenReturn(mock(dev.langchain4j.service.TokenStream.class));
		simple.stream("你好");
		verify(assistant).stream("0:crm:bot:u1:s-7", "", contents("你好"));
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

	// -------------------------------------------------- 会话维度（5 段）

	@Test
	void memoryIdStaysFourSegmentsWithSession() {
		simple.dimensions("t1", "crm", "bot", "u1").session("s1");
		// 四段契约保持不变（既有断言），会话维度另经 effectiveMemoryId 暴露
		assertThat(simple.memoryId()).isEqualTo("t1:crm:bot:u1");
	}

	@Test
	void effectiveMemoryIdIsFiveSegmentsWithExplicitSession() {
		simple.dimensions("t1", "crm", "bot", "u1").session("s1");
		assertThat(simple.effectiveMemoryId()).isEqualTo("t1:crm:bot:u1:s1");
	}

	@Test
	void explicitSessionWinsOverResolver() {
		simple.dimensions("t1", "crm", "bot", "u1").session("conversation-42");
		assertThat(simple.sessionId()).isEqualTo("conversation-42");
		assertThat(simple.effectiveMemoryId()).isEqualTo("t1:crm:bot:u1:conversation-42");
	}

	@Test
	void missingSessionFallsBackToDailyResolver() {
		simple.dimensions("t1", "crm", "bot", "u1");
		// 未显式提供会话时按天兜底，最终键恒为 5 段
		assertThat(simple.sessionId()).matches("d\\d{8}");
		assertThat(simple.effectiveMemoryId()).matches("t1:crm:bot:u1:d\\d{8}");
	}

	@Test
	void sessionRejectsSeparator() {
		assertThatThrownBy(() -> simple.session("s:1"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("分隔符");
	}
}
