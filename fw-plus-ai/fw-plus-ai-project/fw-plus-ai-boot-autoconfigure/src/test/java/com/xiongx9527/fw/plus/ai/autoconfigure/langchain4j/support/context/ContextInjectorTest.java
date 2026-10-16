package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一上下文注入器：来源可插拔、无内容跳过、失败隔离、三种组合策略。
 */
class ContextInjectorTest {

	private final ContextRequest request = new ContextRequest("t1:crm:bot:u1:s1", "t1:crm:bot:u1:s1",
			"u1", "你好");

	@Test
	void emptyProviderProducesNoBlock() {
		ContextProvider empty = r -> "";
		ContextProvider blank = r -> "   ";
		ContextProvider value = r -> "摘要内容";

		ContextInjector injector = new ContextInjector(
				Arrays.asList(empty, blank, value), ContextStrategy.MEMORY_AND_ENRICHED);

		assertThat(injector.render(request)).isEqualTo("摘要内容");
	}

	@Test
	void allEmptyProvidersRenderEmpty() {
		ContextInjector injector = new ContextInjector(
				Collections.singletonList(r -> null), ContextStrategy.MEMORY_AND_ENRICHED);
		assertThat(injector.render(request)).isEmpty();
	}

	@Test
	void memoryStrategyInjectsNothing() {
		ContextInjector injector = new ContextInjector(
				Collections.singletonList(r -> "摘要"), ContextStrategy.MEMORY);
		assertThat(injector.render(request)).isEmpty();
		assertThat(ContextStrategy.MEMORY.injectsEnriched()).isFalse();
		assertThat(ContextStrategy.MEMORY.keepsMemory()).isTrue();
	}

	@Test
	void enrichedStrategyInjectsProviders() {
		ContextInjector injector = new ContextInjector(
				Collections.singletonList(r -> "摘要"), ContextStrategy.ENRICHED);
		assertThat(injector.render(request)).isEqualTo("摘要");
		assertThat(ContextStrategy.ENRICHED.keepsMemory()).isFalse();
	}

	@Test
	void multipleProvidersJoinedInOrder() {
		ContextInjector injector = new ContextInjector(
				Arrays.asList(r -> "事实", r -> "摘要"), ContextStrategy.MEMORY_AND_ENRICHED);
		assertThat(injector.render(request)).isEqualTo("事实\n\n摘要");
	}

	@Test
	void failingProviderIsIsolated() {
		ContextProvider boom = r -> {
			throw new IllegalStateException("provider 挂了");
		};
		ContextProvider ok = r -> "仍然可用";

		ContextInjector injector = new ContextInjector(
				Arrays.asList(boom, ok), ContextStrategy.MEMORY_AND_ENRICHED);

		assertThat(injector.render(request)).isEqualTo("仍然可用");
	}

	@Test
	void newProviderNeedsNoInjectorChange() {
		// 注册新来源无需改动注入逻辑：顺序拼接即可纳入
		ContextInjector injector = new ContextInjector(
				Arrays.asList(r -> "A", r -> "B", r -> "C"), ContextStrategy.MEMORY_AND_ENRICHED);
		assertThat(injector.render(request)).isEqualTo("A\n\nB\n\nC");
	}
}