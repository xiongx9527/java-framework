package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 摘要策略注册表：5 个内置策略全实现、按名字命中、按 FQCN 反射、未知名返回 null。
 */
class SummarizersTest {

	private final SummarizeRequest request = new SummarizeRequest("既有摘要：用户叫小明",
			Arrays.asList("用户：帮我订明天的票", "助手：好的，请提供出发地"),
			"用户：帮我订明天的票\n助手：好的，请提供出发地", 800);

	@Test
	void builtInNamesAllResolve() {
		for (String name : Arrays.asList(Summarizers.REFINE, Summarizers.REBUILD,
				Summarizers.MAP_REDUCE, Summarizers.SEGMENTED, Summarizers.STRUCTURED)) {
			assertThat(Summarizers.REGISTRY.isBuiltIn(name)).isTrue();
			assertThat(Summarizers.resolve(name, null)).isNotNull();
		}
	}

	@Test
	void everyBuiltInProducesNonEmptySummary() {
		for (String name : Arrays.asList(Summarizers.REFINE, Summarizers.REBUILD,
				Summarizers.MAP_REDUCE, Summarizers.SEGMENTED, Summarizers.STRUCTURED)) {
			Summarizer summarizer = Summarizers.resolve(name, null);
			assertThat(summarizer.summarize(request))
					.as("策略 %s 应产出非空摘要", name)
					.isNotBlank();
		}
	}

	@Test
	void modelIsUsedWhenProvided() {
		SummaryModel model = prompt -> "模型摘要";
		Summarizer summarizer = Summarizers.resolve(Summarizers.REFINE, model);
		assertThat(summarizer.summarize(request)).isEqualTo("模型摘要");
	}

	@Test
	void fqcnResolvesViaReflection() {
		Summarizer summarizer = Summarizers.resolve(NoArgSummarizer.class.getName(), null);
		assertThat(summarizer).isNotNull();
		assertThat(summarizer.summarize(request)).isEqualTo("反射摘要");
	}

	@Test
	void unknownNameReturnsNull() {
		assertThat(Summarizers.resolve("does-not-exist", null)).isNull();
	}

	@Test
	void resolveOrDefaultFallsBackToRefine() {
		assertThat(Summarizers.resolveOrDefault("does-not-exist", null).name())
				.isEqualTo(Summarizers.REFINE);
	}

	/** 供反射用例：无参构造的自定义策略 */
	public static class NoArgSummarizer implements Summarizer {

		@Override
		public String name() {
			return "no-arg";
		}

		@Override
		public String summarize(SummarizeRequest request) {
			return "反射摘要";
		}
	}
}