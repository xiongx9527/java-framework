package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link IntentAwareToolProvider} 的流式构造器：从一个「全量工具来源」取工具，再按当前意图裁剪成白名单。
 *
 * <p>两种意图来源与 {@link IntentRag} 一致：{@code byIntent(classifier)} 每次装配现跑分类；
 * {@code bySupplier(supplier)} 复用已判定的意图（同一次问答 RAG 与工具只判一次）。</p>
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * // 物流意图只给查单/物流工具，闲聊不给工具，转人工工具始终保留，其余意图放开全量
 * ToolProvider tools = IntentTools.byIntent(classifier)
 *         .source(new AggregatingToolProvider(providerA, providerB)) // 单来源：多提供者先聚合
 *         .allow(MyIntent.LOGISTICS, "track_order", "query_logistics")
 *         .allowNone(MyIntent.CHITCHAT)
 *         .alwaysInclude("handoff_to_human")
 *         .fallbackAll()
 *         .build();
 *
 * // 使用结果：就是一个普通 ToolProvider
 * ToolProviderResult result = tools.provideTools(request);
 * }</pre>
 *
 * <p>裁剪语义（命中意图优先于兜底）：</p>
 * <ul>
 *   <li>命中且 {@link Builder#allow allow(i, ...)} → 白名单 + {@code alwaysInclude}；</li>
 *   <li>命中且 {@link Builder#allowNone allowNone(i)} → 仅 {@code alwaysInclude}；</li>
 *   <li>未匹配或匹配到但未配置 → {@link Builder#fallbackAll()} 全量 /
 *       {@link Builder#fallback fallback(...)} 指定集合 / {@link Builder#fallbackNone()}（默认）仅
 *       {@code alwaysInclude}；</li>
 *   <li>{@code allow(i, ...)} 与 {@code allowNone(i)} 对同一意图互斥，后调用者生效。</li>
 * </ul>
 *
 * <p>裁剪按工具的 {@link dev.langchain4j.agent.tool.ToolSpecification#name()} 匹配；
 * 全量来源抛错 → 输出空工具集；意图识别失败 → 按「未匹配」兜底。默认兜底为
 * {@link #fallbackNone()}（未匹配时只保留 {@code alwaysInclude}），即「宁可少给工具」，
 * 需要放开时显式调用 {@link #fallbackAll()} 或 {@link #fallback(String...)}。</p>
 *
 * <p>接入方式：它就是一个 {@link ToolProvider}——直接交给 {@code AssistantBuilder.toolProvider(...)}。</p>
 */
public final class IntentTools {

	private IntentTools() {
	}

	/** 以分类器为意图来源：每次装配工具现跑一遍分类。 */
	public static <I extends Enum<I>> Builder<I> byIntent(IntentClassifier<I> classifier) {
		if (classifier == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.IntentTools.classifierRequired"));
		}
		return bySupplier(IntentSupplier.of(classifier));
	}

	/** 以调用方提供的意图来源构建（可复用已判定的意图，避免重复识别）。 */
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
				throw new IllegalArgumentException(
						I18nMessageResolver.resolveKey("fw.ai.IntentTools.intentSourceRequired"));
			}
			this.supplier = supplier;
		}

		/**
		 * 全量工具来源（如 {@code AggregatingToolProvider}）。<b>单值</b>，后调用的覆盖先调用的；
		 * 要合并多个提供者，先用 {@code new AggregatingToolProvider(...)} 聚合成一个再传入。
		 */
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
				throw new IllegalArgumentException(
						I18nMessageResolver.resolveKey("fw.ai.IntentTools.intentRequired"));
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
