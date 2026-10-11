package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link IntentAwareContentRetriever} 的流式构造器。
 *
 * <pre>{@code
 * ContentRetriever retriever = IntentRag.byIntent(classifier)
 *         .route(MyIntent.LOGISTICS, logisticsRetriever)
 *         .routeNone(MyIntent.CHITCHAT)
 *         .fallback(defaultRetriever)
 *         .build();
 * }</pre>
 *
 * <p>{@code byIntent(classifier)} 每次检索现跑分类；{@code bySupplier(supplier)} 复用调用方已判定的
 * 意图（避免一次问答里 RAG 与工具各判定一次）。</p>
 */
public final class IntentRag {

	private IntentRag() {
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

		private final Map<I, ContentRetriever> routes = new LinkedHashMap<I, ContentRetriever>();

		private final Set<I> noRoute = new LinkedHashSet<I>();

		private ContentRetriever fallback;

		private Builder(IntentSupplier<I> supplier) {
			if (supplier == null) {
				throw new IllegalArgumentException("意图来源不能为 null");
			}
			this.supplier = supplier;
		}

		/** 为某意图绑定专属检索器。 */
		public Builder<I> route(I intent, ContentRetriever retriever) {
			if (intent == null) {
				throw new IllegalArgumentException("intent 不能为 null");
			}
			if (retriever == null) {
				throw new IllegalArgumentException("retriever 不能为 null");
			}
			routes.put(intent, retriever);
			noRoute.remove(intent);
			return this;
		}

		/** 显式声明某意图「不检索」（如闲聊、澄清类意图无需外部知识）。 */
		public Builder<I> routeNone(I intent) {
			if (intent == null) {
				throw new IllegalArgumentException("intent 不能为 null");
			}
			noRoute.add(intent);
			routes.remove(intent);
			return this;
		}

		public Builder<I> routeNone(Collection<I> intents) {
			if (intents != null) {
				for (I intent : intents) {
					routeNone(intent);
				}
			}
			return this;
		}

		/** 未匹配到意图时的兜底检索器（默认库）。 */
		public Builder<I> fallback(ContentRetriever retriever) {
			this.fallback = retriever;
			return this;
		}

		/** 未匹配到意图时显式不检索（默认行为）。 */
		public Builder<I> fallbackNone() {
			this.fallback = null;
			return this;
		}

		public IntentAwareContentRetriever<I> build() {
			return new IntentAwareContentRetriever<I>(supplier, routes, noRoute, fallback);
		}
	}
}
