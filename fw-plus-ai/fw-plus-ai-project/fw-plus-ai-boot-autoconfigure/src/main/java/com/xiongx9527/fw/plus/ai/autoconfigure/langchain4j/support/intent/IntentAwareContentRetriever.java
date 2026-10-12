package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 意图化检索器：按当前意图把检索请求路由到对应子检索器，实现「不同意图查不同知识库」。
 *
 * <p>三态路由：</p>
 * <ul>
 *   <li>命中意图且配置了专属检索器 → 路由到该检索器；</li>
 *   <li>命中意图但显式声明「不检索」（见 {@link IntentRag.Builder#routeNone}）→ 直接返回空列表；</li>
 *   <li>未匹配到意图（或匹配到但未配置）→ 走兜底：默认库检索（{@code fallback(retriever)}）或
 *       显式不检索（{@code fallbackNone()}）。</li>
 * </ul>
 *
 * <p>意图识别或子检索器抛错时降级为空结果并记日志，不让一次检索失败拖垮整轮对话。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class IntentAwareContentRetriever<I extends Enum<I>> implements ContentRetriever {

	private static final Logger logger = LoggerFactory.getLogger(IntentAwareContentRetriever.class);

	private final IntentSupplier<I> intentSupplier;

	private final Map<I, ContentRetriever> routes;

	private final Set<I> noRoute;

	private final ContentRetriever fallback;

	public IntentAwareContentRetriever(IntentSupplier<I> intentSupplier, Map<I, ContentRetriever> routes,
			Set<I> noRoute, ContentRetriever fallback) {
		if (intentSupplier == null) {
			throw new IllegalArgumentException("intentSupplier 不能为 null");
		}
		this.intentSupplier = intentSupplier;
		this.routes = routes == null
				? Collections.<I, ContentRetriever>emptyMap()
				: new LinkedHashMap<I, ContentRetriever>(routes);
		this.noRoute = noRoute == null
				? Collections.<I>emptySet()
				: new LinkedHashSet<I>(noRoute);
		this.fallback = fallback;
	}

	@Override
	public List<Content> retrieve(Query query) {
		I intent = resolveIntent(query);
		ContentRetriever target = route(intent);
		if (target == null) {
			return Collections.emptyList();
		}
		try {
			List<Content> contents = target.retrieve(query);
			return contents == null ? Collections.<Content>emptyList() : contents;
		} catch (RuntimeException e) {
			logger.warn("意图化检索：意图 {} 对应检索器执行失败，降级为空结果", intent, e);
			return Collections.emptyList();
		}
	}

	private I resolveIntent(Query query) {
		try {
			return intentSupplier.intent(IntentContext.of(query == null ? null : query.text()));
		} catch (RuntimeException e) {
			logger.warn("意图化检索：意图识别失败，按未匹配兜底", e);
			return null;
		}
	}

	private ContentRetriever route(I intent) {
		if (intent != null) {
			if (noRoute.contains(intent)) {
				return null;
			}
			ContentRetriever routed = routes.get(intent);
			if (routed != null) {
				return routed;
			}
		}
		return fallback;
	}

	public Map<I, ContentRetriever> routes() {
		return Collections.unmodifiableMap(routes);
	}

	public Set<I> noRoute() {
		return Collections.unmodifiableSet(noRoute);
	}
}
