package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 意图化工具提供者：按意图裁剪（allow / allowNone）、alwaysInclude 叠加、未匹配兜底三态。
 */
class IntentAwareToolProviderTest {

	private static final ToolProviderRequest REQUEST =
			new ToolProviderRequest("memory-1", UserMessage.from("查物流"));

	private static ToolProvider source() {
		return request -> {
			ToolProviderResult.Builder builder = ToolProviderResult.builder();
			builder.add(spec("track_order"), executor());
			builder.add(spec("query_logistics"), executor());
			builder.add(spec("create_refund"), executor());
			builder.add(spec("handoff_to_human"), executor());
			return builder.build();
		};
	}

	private static ToolSpecification spec(String name) {
		return ToolSpecification.builder().name(name).description(name).build();
	}

	private static ToolExecutor executor() {
		return (request, memoryId) -> "ok";
	}

	private static Set<String> names(ToolProviderResult result) {
		return result.tools().keySet().stream()
				.map(ToolSpecification::name)
				.collect(Collectors.toSet());
	}

	@Test
	void allowKeepsOnlyConfiguredTools() {
		ToolProvider provider = IntentTools.<SampleIntent>bySupplier(context -> SampleIntent.LOGISTICS)
				.source(source())
				.allow(SampleIntent.LOGISTICS, "track_order", "query_logistics")
				.build();

		assertThat(names(provider.provideTools(REQUEST)))
				.containsExactlyInAnyOrder("track_order", "query_logistics");
	}

	@Test
	void allowNoneKeepsNothing() {
		ToolProvider provider = IntentTools.<SampleIntent>bySupplier(context -> SampleIntent.CHITCHAT)
				.source(source())
				.allowNone(SampleIntent.CHITCHAT)
				.build();

		assertThat(names(provider.provideTools(REQUEST))).isEmpty();
	}

	@Test
	void alwaysIncludeIsAddedOnEveryBranch() {
		ToolProvider provider = IntentTools.<SampleIntent>bySupplier(context -> SampleIntent.CHITCHAT)
				.source(source())
				.allowNone(SampleIntent.CHITCHAT)
				.alwaysInclude("handoff_to_human")
				.build();

		assertThat(names(provider.provideTools(REQUEST))).containsExactly("handoff_to_human");
	}

	@Test
	void unmatchedFallsBackToAllTools() {
		ToolProvider provider = IntentTools.<SampleIntent>bySupplier(context -> null)
				.source(source())
				.allow(SampleIntent.LOGISTICS, "track_order")
				.fallbackAll()
				.build();

		assertThat(names(provider.provideTools(REQUEST)))
				.containsExactlyInAnyOrder("track_order", "query_logistics", "create_refund", "handoff_to_human");
	}

	@Test
	void unmatchedFallsBackToNoneByDefault() {
		ToolProvider provider = IntentTools.<SampleIntent>bySupplier(context -> SampleIntent.HUMAN)
				.source(source())
				.allow(SampleIntent.LOGISTICS, "track_order")
				.build();

		assertThat(names(provider.provideTools(REQUEST))).isEmpty();
	}

	@Test
	void unmatchedFallsBackToConfiguredSet() {
		ToolProvider provider = IntentTools.<SampleIntent>bySupplier(context -> SampleIntent.HUMAN)
				.source(source())
				.fallback("create_refund")
				.alwaysInclude("handoff_to_human")
				.build();

		assertThat(names(provider.provideTools(REQUEST)))
				.containsExactlyInAnyOrder("create_refund", "handoff_to_human");
	}

	@Test
	void sourceFailureDegradesToEmpty() {
		ToolProvider broken = request -> {
			throw new IllegalStateException("工具来源不可用");
		};
		ToolProvider provider = IntentTools.<SampleIntent>bySupplier(context -> SampleIntent.LOGISTICS)
				.source(broken)
				.fallbackAll()
				.build();

		assertThat(names(provider.provideTools(REQUEST))).isEmpty();
	}

	@Test
	void intentSupplierFailureDegradesToFallback() {
		IntentSupplier<SampleIntent> broken = context -> {
			throw new IllegalStateException("分类器不可用");
		};
		ToolProvider provider = IntentTools.bySupplier(broken)
				.source(source())
				.fallbackAll()
				.build();

		assertThat(names(provider.provideTools(REQUEST)))
				.containsExactlyInAnyOrder("track_order", "query_logistics", "create_refund", "handoff_to_human");
	}
}
