package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.InMemorySummaryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary.SummaryMemoryAssembler;
import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 任务 4.1：事实装配器——关闭不动 builder、开启但无存储时跳过、开启且可用时注册
 * 事实来源与维护钩子，且注册顺序先于摘要来源（保证注入渲染顺序
 * 人设 → 事实 → 摘要 → 最近 N 条原文）。
 */
class FactMemoryAssemblerTest {

	@Test
	void disabledFactsDoesNotTouchBuilder() {
		AssistantBuilder builder = mock(AssistantBuilder.class);
		Langchain4jAssistantProperties properties = new Langchain4jAssistantProperties();

		assertThat(FactMemoryAssembler.apply(builder, properties, new InMemoryFactStore(), null, null)).isFalse();
		verifyNoInteractions(builder);
	}

	@Test
	void nullPropertiesIsNoOp() {
		AssistantBuilder builder = mock(AssistantBuilder.class);

		assertThat(FactMemoryAssembler.apply(builder, null, new InMemoryFactStore(), null, null)).isFalse();
		verifyNoInteractions(builder);
	}

	@Test
	void enabledWithoutStoreSkipsWithoutRegistering() {
		AssistantBuilder builder = mock(AssistantBuilder.class);

		assertThat(FactMemoryAssembler.apply(builder, enabledProperties(), null, null, null)).isFalse();
		verifyNoInteractions(builder);
	}

	@Test
	void enabledWiresProviderAndMaintenance() {
		AssistantBuilder builder = mock(AssistantBuilder.class);

		boolean applied = FactMemoryAssembler.apply(builder, enabledProperties(), new InMemoryFactStore(), null, null);

		assertThat(applied).isTrue();
		verify(builder).contextProvider(any(FactContextProvider.class));
		verify(builder).contextMaintenance(any(FactMaintenance.class));
	}

	@Test
	void unknownExtractorFailsFast() {
		Langchain4jAssistantProperties properties = enabledProperties();
		properties.getSession().getFacts().setExtractor("unknown-strategy");

		assertThatThrownBy(() -> FactMemoryAssembler.apply(mock(AssistantBuilder.class), properties,
				new InMemoryFactStore(), null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("unknown-strategy");
	}

	@Test
	void factsProviderRegisteredBeforeSummaryProvider() {
		AssistantBuilder builder = mock(AssistantBuilder.class);
		Langchain4jAssistantProperties properties = enabledProperties();
		properties.getSession().setEnabled(true);
		properties.getSession().getSummary().setEnabled(true);

		assertThat(FactMemoryAssembler.apply(builder, properties, new InMemoryFactStore(), null, null)).isTrue();
		assertThat(SummaryMemoryAssembler.apply(builder, properties, new InMemorySummaryStore(), null, null, null))
				.isTrue();

		InOrder inOrder = inOrder(builder);
		inOrder.verify(builder).contextProvider(any(FactContextProvider.class));
		inOrder.verify(builder).contextProvider(any(SummaryContextProvider.class));
	}

	private static Langchain4jAssistantProperties enabledProperties() {
		Langchain4jAssistantProperties properties = new Langchain4jAssistantProperties();
		properties.getSession().getFacts().setEnabled(true);
		return properties;
	}

	/** 内存假存储：装配测试不关心网络行为 */
	static final class InMemoryFactStore implements FactStore {

		@Override
		public void addDialogue(List<ChatMessage> turnMessages, String userId) {
		}

		@Override
		public void addFacts(List<String> facts, String userId) {
		}

		@Override
		public List<String> search(String query, String userId, int topK) {
			return new ArrayList<String>();
		}

		@Override
		public List<String> listAll(String userId, int limit) {
			return new ArrayList<String>();
		}
	}

}
