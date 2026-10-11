package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.service.tool.ToolProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link IntentAwareToolProvider} 的流式构造器。
 *
 * <pre>{@code
 * ToolProvider tools = IntentTools.byIntent(classifier)
 *         .source(aggregatingToolProvider)
 *         .allow(MyIntent.LOGISTICS, "track_order", "query_logistics")
 *         .allowNone(MyIntent.CHITCHAT)
 *         .alwaysInclude("handoff_to_human")
 *         .fallbackAll()
 *         .build();
 * }</pre>
 *
 * <p>默认兜底为 {@link #fallbackNone()}（未匹配意图时只保留 {@code alwaysInclude}），
 * 即「宁可少给工具」，需要放开时显式调用 {@link #fallbackAll()} 或 {@link #fallback(String...)}。</p>
 */
public final class IntentTools {

	private IntentTools() {
	}

	public static <I extends Enum<I>> Builder<I> byIntent(IntentClassifier<I> classifier) {
		if (classifier == null) {
			throw new IllegalArgumentException("classifier 不能为 null");
		}
		return bySupplier(IntentSupplier.of(classifier));
	}

	public static <I extends Enum<I>> Builder<I> bySupplier(IntentSupplier<I> supplier) {
		return new Builder<I>(supplier);
	}

	public static final class Builder<I extends Enum<I>> {

		private final IntentSupplier<I> supplier;

		private final Map<I, Set<String>> allowed = new LinkedHashMap<I, Set<String>>();

		private final Set<I> allowNone = new LinkedHashSet<I>();

		private final Set<String> alwaysInclude = new LinkedHashSet<String>();

		private final Set<String> fallbackTools = new LinkedHashSet<String>();

		private ToolProvider source;

		private IntentAwareToolProvider.FallbackMode fallbackMode = IntentAwareToolProvider.FallbackMode.NONE;

		private Builder(IntentSupplier<I> supplier) {
			if (supplier == null) {
				throw new IllegalArgumentException("意图来源不能为 null");
			}
			this.supplier = supplier;
		}

		/** 全量工具来源（如 {@code AggregatingToolProvider}）。 */
		public Builder<I> source(ToolProvider source) {
			this.source = source;
			return this;
		}

		/** 为某意图指定允许的工具名集合。 */
		public Builder<I> allow(I intent, String... toolNames) {
			require(intent);
			allowed.put(intent, toSet(toolNames));
			allowNone.remove(intent);
			return this;
		}

		/** 显式声明某意图不开放任何工具（只剩 {@code alwaysInclude}）。 */
		public Builder<I> allowNone(I intent) {
			require(intent);
			allowNone.add(intent);
			allowed.remove(intent);
			return this;
		}

		public Builder<I> allowNone(Collection<I> intents) {
			if (intents != null) {
				for (I intent : intents) {
					allowNone(intent);
				}
			}
			return this;
		}

		/** 无论命中与否都保留的工具（叠加到任何分支）。 */
		public Builder<I> alwaysInclude(String... toolNames) {
			alwaysInclude.addAll(toSet(toolNames));
			return this;
		}

		/** 未匹配意图时保留全部工具。 */
		public Builder<I> fallbackAll() {
			this.fallbackMode = IntentAwareToolProvider.FallbackMode.ALL;
			this.fallbackTools.clear();
			return this;
		}

		/** 未匹配意图时只保留 {@code alwaysInclude}（默认）。 */
		public Builder<I> fallbackNone() {
			this.fallbackMode = IntentAwareToolProvider.FallbackMode.NONE;
			this.fallbackTools.clear();
			return this;
		}

		/** 未匹配意图时保留指定工具（叠加 {@code alwaysInclude}）。 */
		public Builder<I> fallback(String... toolNames) {
			this.fallbackMode = IntentAwareToolProvider.FallbackMode.SET;
			this.fallbackTools.clear();
			this.fallbackTools.addAll(toSet(toolNames));
			return this;
		}

		public IntentAwareToolProvider<I> build() {
			return new IntentAwareToolProvider<I>(supplier, source, allowed, allowNone, alwaysInclude,
					fallbackMode, fallbackTools);
		}

		private static void require(Object intent) {
			if (intent == null) {
				throw new IllegalArgumentException("intent 不能为 null");
			}
		}

		private static Set<String> toSet(String... names) {
			Set<String> set = new LinkedHashSet<String>();
			if (names != null) {
				set.addAll(Arrays.asList(names));
			}
			return set;
		}
	}
}
