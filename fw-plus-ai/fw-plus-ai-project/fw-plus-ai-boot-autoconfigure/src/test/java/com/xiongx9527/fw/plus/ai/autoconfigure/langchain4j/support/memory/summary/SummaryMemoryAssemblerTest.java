package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextMaintenance;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session.SessionIdResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 会话/摘要装配器：把 {@code session.*} / {@code session.summary.*} 配置真正落到 builder。
 * 这是「类已建但无消费方」的收口点，缺了它配置全是死配置。
 */
class SummaryMemoryAssemblerTest {

	@Test
	void disabledSessionDoesNotTouchBuilder() {
		AssistantBuilder builder = mock(AssistantBuilder.class);
		Langchain4jAssistantProperties properties = new Langchain4jAssistantProperties();
		properties.getSession().setEnabled(false);

		boolean applied = SummaryMemoryAssembler.apply(builder, properties, null, null, null, null);

		assertThat(applied).isFalse();
		verifyNoInteractions(builder);
	}

	@Test
	void nullPropertiesIsNoOp() {
		AssistantBuilder builder = mock(AssistantBuilder.class);

		assertThat(SummaryMemoryAssembler.apply(builder, null, null, null, null, null)).isFalse();
		verifyNoInteractions(builder);
	}

	@Test
	void sessionOnlyWiresResolverAndStrategyWithoutSummary() {
		AssistantBuilder builder = mock(AssistantBuilder.class);
		Langchain4jAssistantProperties properties = new Langchain4jAssistantProperties();
		properties.getSession().setContextStrategy("memory");
		properties.getSession().getSummary().setEnabled(false);

		boolean applied = SummaryMemoryAssembler.apply(builder, properties, null, null, null, null);

		assertThat(applied).isFalse();
		verify(builder).sessionResolver(any(SessionIdResolver.class));
		verify(builder).contextStrategy(ContextStrategy.MEMORY);
		verify(builder, never()).contextProvider(any(ContextProvider.class));
		verify(builder, never()).contextMaintenance(any(ContextMaintenance.class));
	}

	@Test
	void summaryEnabledWiresProviderAndMaintenance() {
		AssistantBuilder builder = mock(AssistantBuilder.class);
		Langchain4jAssistantProperties properties = new Langchain4jAssistantProperties();
		properties.getSession().setContextStrategy("memory-and-enriched");
		properties.getSession().getSummary().setEnabled(true);

		boolean applied = SummaryMemoryAssembler.apply(builder, properties, new InMemorySummaryStore(),
				null, null, null);

		assertThat(applied).isTrue();
		verify(builder).sessionResolver(any(SessionIdResolver.class));
		verify(builder).contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED);
		verify(builder).contextProvider(any(SummaryContextProvider.class));
		verify(builder).contextMaintenance(any(SummaryMaintenance.class));
	}

	@Test
	void customSessionResolverWinsOverConfigName() {
		AssistantBuilder builder = mock(AssistantBuilder.class);
		Langchain4jAssistantProperties properties = new Langchain4jAssistantProperties();
		properties.getSession().setSessionIdResolver("daily");
		properties.getSession().getSummary().setEnabled(false);
		SessionIdResolver custom = ctx -> "explicit-session";

		SummaryMemoryAssembler.apply(builder, properties, null, null, custom, null);

		verify(builder).sessionResolver(custom);
	}

	@Test
	void configParserMapsKnownStrategyNames() {
		assertThat(ContextStrategy.fromConfig("memory")).isEqualTo(ContextStrategy.MEMORY);
		assertThat(ContextStrategy.fromConfig("ENRICHED")).isEqualTo(ContextStrategy.ENRICHED);
		assertThat(ContextStrategy.fromConfig("memory_and_enriched")).isEqualTo(ContextStrategy.MEMORY_AND_ENRICHED);
		assertThat(ContextStrategy.fromConfig(null)).isEqualTo(ContextStrategy.MEMORY_AND_ENRICHED);
		assertThat(ContextStrategy.fromConfig("未知")).isEqualTo(ContextStrategy.MEMORY_AND_ENRICHED);
	}

}
