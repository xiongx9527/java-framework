package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link IntentAwareContentRetriever} 的流式构造器：把「意图 → 检索器」的映射装配成一个
 * 可按当前意图路由的 {@link ContentRetriever}。
 *
 * <p>两种意图来源：</p>
 * <ul>
 *   <li>{@code byIntent(classifier)}：每次检索现跑一遍分类；</li>
 *   <li>{@code bySupplier(supplier)}：复用调用方已判定的意图，避免一次问答里 RAG 与工具各判定一次。</li>
 * </ul>
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * // 1. 按意图路由到不同知识库：命中各自专属库，闲聊不检索，其余走默认库
 * ContentRetriever retriever = IntentRag.byIntent(classifier)
 *         .route(MyIntent.LOGISTICS, logisticsRetriever)
 *         .route(MyIntent.REFUND, refundRetriever)
 *         .routeNone(MyIntent.CHITCHAT)
 *         .fallback(defaultRetriever)
 *         .build();
 *
 * // 2. 复用已判定的意图：同一次问答 RAG 与工具只判一次
 * IntentClassifier<MyIntent> classifier = factory.auto(MyIntent.class, rules, examples);
 * ContentRetriever shared = IntentRag.bySupplier(IntentSupplier.of(classifier))
 *         .route(MyIntent.LOGISTICS, logisticsRetriever)
 *         .fallbackNone()
 *         .build();
 *
 * // 3. 使用结果：就是一个普通 ContentRetriever
 * List<Content> contents = retriever.retrieve(Query.from("我的快递到哪了"));
 * }</pre>
 *
 * <p>路由语义：</p>
 * <ul>
 *   <li>{@link Builder#route route(i, r)} 与 {@link Builder#routeNone routeNone(i)} 对同一意图<b>互斥</b>，
 *       后调用者生效（先 {@code route} 后又 {@code routeNone} 则该意图变为不检索，反之亦然）；</li>
 *   <li>未匹配意图（{@code supplier} 返回 null）或匹配到但未配置专属检索器 → 走
 *       {@link Builder#fallback}，缺省（{@link Builder#fallbackNone()}）为<b>不检索</b>；</li>
 *   <li>意图识别失败或专属检索器抛错 → 记日志并降级为空结果，不打断整轮对话。</li>
 * </ul>
 *
 * <p>接入方式：它就是一个 {@link ContentRetriever}——直接交给
 * {@code AssistantBuilder.contentRetriever(...)}，或由 {@code DefaultRetrievalAugmentor} 包装；
 * 需要把多个知识库<b>并行合并</b>时，把它与其他检索器一起交给组合检索器
 * {@code com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag.AggregatingContentRetriever}。</p>
 */
public final class IntentRag {

	private IntentRag() {
	}

	/** 以分类器为意图来源：每次检索现跑一遍分类。 */
	public static <I extends Enum<I>> Builder<I> byIntent(IntentClassifier<I> classifier) {
		if (classifier == null) {
			throw new IllegalArgumentException("classifier 不能为 null");
		}
		return bySupplier(IntentSupplier.of(classifier));
	}

	/** 以调用方提供的意图来源构建（可复用已判定的意图，避免重复识别）。 */
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
