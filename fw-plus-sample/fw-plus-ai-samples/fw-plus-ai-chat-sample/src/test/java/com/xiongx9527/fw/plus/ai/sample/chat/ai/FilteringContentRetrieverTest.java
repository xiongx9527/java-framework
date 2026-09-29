package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FilteringContentRetriever}：score 阈值、metadata 过滤、title 渲染。
 */
class FilteringContentRetrieverTest {

	private static Content doc(String text, Object... keyValues) {
		Metadata metadata = new Metadata();
		for (int i = 0; i < keyValues.length; i += 2) {
			metadata.put((String) keyValues[i], String.valueOf(keyValues[i + 1]));
		}
		return Content.from(TextSegment.from(text, metadata));
	}

	private static Map<String, String> filters(String... keyValues) {
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			map.put(keyValues[i], keyValues[i + 1]);
		}
		return map;
	}

	private static FilteringContentRetriever retriever(List<Content> contents,
			double minScore, Map<String, String> filters) {
		final ContentRetriever delegate = query -> contents;
		return new FilteringContentRetriever(delegate, minScore, filters);
	}

	@Test
	void dropsBelowMinScoreAndKeepsHighScore() {
		FilteringContentRetriever retriever = retriever(Arrays.asList(
				doc("杭州夏季气候", "title", "杭州夏季气候", "score", "0.9"),
				doc("兜底无关文档", "score", "0.4")), 0.5, Collections.<String, String>emptyMap());

		List<Content> kept = retriever.retrieve(Query.from("杭州"));

		assertEquals(1, kept.size());
		assertTrue(kept.get(0).textSegment().text().contains("杭州夏季气候"));
	}

	@Test
	void rendersTitleIntoText() {
		FilteringContentRetriever retriever = retriever(Collections.singletonList(
				doc("杭州夏季炎热潮湿", "title", "杭州夏季气候", "score", "0.9")),
				0.5, Collections.<String, String>emptyMap());

		String text = retriever.retrieve(Query.from("杭州")).get(0).textSegment().text();

		assertTrue(text.startsWith("《杭州夏季气候》"), text);
	}

	@Test
	void keepsEntryWithoutMetadata() {
		FilteringContentRetriever retriever = retriever(Collections.singletonList(
				doc("没有元数据的资料")), 0.5, Collections.<String, String>emptyMap());

		List<Content> kept = retriever.retrieve(Query.from("问题"));

		assertEquals(1, kept.size());
		assertEquals("没有元数据的资料", kept.get(0).textSegment().text());
	}

	@Test
	void filtersByCategoryEquality() {
		FilteringContentRetriever retriever = retriever(Arrays.asList(
				doc("杭州热门景点", "title", "杭州景点", "category", "sight", "score", "0.9"),
				doc("夏季出行建议", "title", "出行建议", "category", "guide", "score", "0.9")),
			0.5, filters("category", "sight"));

		List<Content> kept = retriever.retrieve(Query.from("杭州"));

		assertEquals(1, kept.size());
		assertTrue(kept.get(0).textSegment().text().contains("杭州热门景点"));
	}

	@Test
	void filtersByMultiValueTags() {
		FilteringContentRetriever retriever = retriever(Arrays.asList(
				doc("北京景点", "tags", "北京,防晒", "score", "0.9"),
				doc("杭州景点", "tags", "杭州,避暑", "score", "0.9")),
			0.5, filters("tags", "防晒"));

		List<Content> kept = retriever.retrieve(Query.from("景点"));

		assertEquals(1, kept.size());
		assertTrue(kept.get(0).textSegment().text().contains("北京景点"));
	}

	@Test
	void filtersByKeywordsMultiValue() {
		FilteringContentRetriever retriever = retriever(Arrays.asList(
				doc("文档A", "keywords", "天气;气温", "score", "0.9"),
				doc("文档B", "keywords", "景点;旅游", "score", "0.9")),
			0.5, filters("keywords", "气温"));

		assertEquals(1, retriever.retrieve(Query.from("随便")).size());
	}

	@Test
	void allFiltersMustMatch() {
		FilteringContentRetriever retriever = retriever(Arrays.asList(
				doc("文档A", "category", "sight", "tags", "北京,防晒", "score", "0.9")),
			0.5, filters("category", "sight", "tags", "杭州"));

		assertTrue(retriever.retrieve(Query.from("随便")).isEmpty(), "任一条件不满足即淘汰");
	}

	@Test
	void missingMetadataKeyFailsFilter() {
		FilteringContentRetriever retriever = retriever(Collections.singletonList(
				doc("文档A", "score", "0.9")), 0.5, filters("category", "sight"));

		assertTrue(retriever.retrieve(Query.from("随便")).isEmpty(), "没有该字段就淘汰");
	}



	@Test
	void emptyResultWhenDelegateReturnsNothing() {
		FilteringContentRetriever retriever = retriever(Collections.<Content>emptyList(),
				0.5, filters("category", "sight"));

		assertTrue(retriever.retrieve(Query.from("问题")).isEmpty());
	}

	@Test
	void missingScoreTreatedAsHighest() {
		FilteringContentRetriever retriever = retriever(Collections.singletonList(
				doc("无 score 的文档", "title", "标题")), 0.5, Collections.<String, String>emptyMap());

		assertEquals(1, retriever.retrieve(Query.from("问题")).size(), "score 缺失按 1.0 处理");
	}
}
