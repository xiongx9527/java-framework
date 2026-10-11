package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 意图化检索器：三态路由（命中专属 / 显式不检索 / 未匹配兜底）与失败降级。
 */
class IntentAwareContentRetrieverTest {

	private static final Query QUERY = Query.from("查物流");

	private static ContentRetriever tag(String tag) {
		return query -> Collections.singletonList(Content.from(tag));
	}

	private static ContentRetriever router(IntentSupplier<SampleIntent> supplier) {
		return IntentRag.bySupplier(supplier)
				.route(SampleIntent.LOGISTICS, tag("logistics"))
				.route(SampleIntent.REFUND, tag("refund"))
				.build();
	}

	private static List<String> texts(List<Content> contents) {
		return contents.stream()
				.map(content -> content.textSegment().text())
				.collect(Collectors.toList());
	}

	@Test
	void routesToIntentSpecificRetriever() {
		ContentRetriever router = router(context -> SampleIntent.LOGISTICS);

		assertThat(texts(router.retrieve(QUERY))).containsExactly("logistics");
	}

	@Test
	void routeNoneYieldsNoContent() {
		ContentRetriever router = IntentRag.<SampleIntent>bySupplier(context -> SampleIntent.CHITCHAT)
				.routeNone(SampleIntent.CHITCHAT)
				.fallback(tag("fallback"))
				.build();

		assertThat(router.retrieve(QUERY)).isEmpty();
	}

	@Test
	void unmatchedFallsBackToDefaultRetriever() {
		ContentRetriever router = IntentRag.<SampleIntent>bySupplier(context -> null)
				.route(SampleIntent.LOGISTICS, tag("logistics"))
				.fallback(tag("fallback"))
				.build();

		assertThat(texts(router.retrieve(QUERY))).containsExactly("fallback");
	}

	@Test
	void unmatchedWithFallbackNoneYieldsNoContent() {
		ContentRetriever router = IntentRag.<SampleIntent>bySupplier(context -> SampleIntent.HUMAN)
				.route(SampleIntent.LOGISTICS, tag("logistics"))
				.fallbackNone()
				.build();

		assertThat(router.retrieve(QUERY)).isEmpty();
	}

	@Test
	void delegateFailureDegradesToEmpty() {
		ContentRetriever broken = query -> {
			throw new IllegalStateException("检索器不可用");
		};
		ContentRetriever router = IntentRag.<SampleIntent>bySupplier(context -> SampleIntent.LOGISTICS)
				.route(SampleIntent.LOGISTICS, broken)
				.build();

		assertThat(router.retrieve(QUERY)).isEmpty();
	}

	@Test
	void intentSupplierFailureDegradesToFallback() {
		IntentSupplier<SampleIntent> broken = context -> {
			throw new IllegalStateException("分类器不可用");
		};
		ContentRetriever router = IntentRag.bySupplier(broken)
				.route(SampleIntent.LOGISTICS, tag("logistics"))
				.fallback(tag("fallback"))
				.build();

		assertThat(texts(router.retrieve(QUERY))).containsExactly("fallback");
	}
}
