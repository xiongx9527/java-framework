package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.dataset.entity.Document;
import com.xiongx9527.fw.plus.dify.dataset.entity.Segment;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrieveResult;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeMetadataCondition;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DifyContentRetriever}：多库并行检索、分数合并截断、故障降级、
 * chatMemoryId 四段 → metadata_filtering_conditions 过滤。
 */
class DifyContentRetrieverTest {

	private final IDatasetClient client = mock(IDatasetClient.class);

	// -------------------------------------------------- 基础检索

	@Test
	void mergesDatasetsSortedByScoreDesc() throws Exception {
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(
				result(record(0.5, "甲"), record(0.9, "乙")));
		when(client.retrieve(eq("ds-2"), anyString(), any())).thenReturn(
				result(record(0.7, "丙")));

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties("ds-1", "ds-2"))) {
			List<Content> contents = retriever.retrieve(Query.from("问题"));

			// 多库按分数降序合并
			assertThat(textsOf(contents)).containsExactly("乙", "丙", "甲");
			assertThat(metadataOf(contents.get(0), "datasetId")).isEqualTo("ds-1");
			assertThat(metadataOf(contents.get(1), "datasetId")).isEqualTo("ds-2");
		}
	}

	@Test
	void truncatesToTopK() throws Exception {
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(
				result(record(0.9, "a"), record(0.8, "b"), record(0.7, "c")));

		DifyRagProperties properties = properties("ds-1");
		properties.setTopK(2);
		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties)) {
			assertThat(textsOf(retriever.retrieve(Query.from("问题")))).containsExactly("a", "b");
		}
	}

	@Test
	void nullScoreSortedLast() throws Exception {
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(
				result(record(null, "无分"), record(0.5, "有分")));

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties("ds-1"))) {
			assertThat(textsOf(retriever.retrieve(Query.from("问题")))).containsExactly("有分", "无分");
		}
	}

	@Test
	void degradesToEmptyWhenSingleDatasetFails() throws Exception {
		when(client.retrieve(anyString(), anyString(), any())).thenThrow(new RuntimeException("connection refused"));

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties("ds-1"))) {
			assertThat(retriever.retrieve(Query.from("问题"))).isEmpty();
		}
	}

	@Test
	void skipsFailedDatasetAndKeepsOthers() throws Exception {
		// 并行扇出：一路失败只跳过该库，不影响其余库的结果
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(result(record(0.8, "存活")));
		when(client.retrieve(eq("ds-2"), anyString(), any())).thenThrow(new RuntimeException("timeout"));

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties("ds-1", "ds-2"))) {
			assertThat(textsOf(retriever.retrieve(Query.from("问题")))).containsExactly("存活");
		}
	}

	@Test
	void nullQueryReturnsEmpty() throws Exception {
		// Query.from 自身拒绝空白文本，空白分支不可达，这里覆盖 query 为 null 的防御路径
		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties("ds-1"))) {
			assertThat(retriever.retrieve(null)).isEmpty();
		}
		verify(client, never()).retrieve(anyString(), anyString(), any());
	}

	// -------------------------------------------------- 结果映射

	@Test
	void mapsSegmentMetadata() throws Exception {
		Document document = new Document();
		document.setName("文档A");
		Segment segment = Segment.builder()
				.id("seg-1").content("正文")
				.answer("答案")
				.keywords(Arrays.asList("k1", "k2"))
				.document(document)
				.build();
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(result(record(0.9, null, segment)));

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties("ds-1"))) {
			List<Content> contents = retriever.retrieve(Query.from("问题"));
			assertThat(contents).hasSize(1);
			Content content = contents.get(0);
			assertThat(content.textSegment().text()).isEqualTo("正文");
			assertThat(metadataOf(content, "score")).isEqualTo("0.9");
			assertThat(metadataOf(content, "datasetId")).isEqualTo("ds-1");
			assertThat(metadataOf(content, "segmentId")).isEqualTo("seg-1");
			assertThat(metadataOf(content, "title")).isEqualTo("文档A");
			assertThat(metadataOf(content, "answer")).isEqualTo("答案");
			assertThat(metadataOf(content, "keywords")).isEqualTo("k1,k2");
		}
	}

	// -------------------------------------------------- 元数据过滤（chatMemoryId 四段）

	@Test
	void buildsMetadataConditionFromFourSegmentMemoryId() throws Exception {
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(result(record(0.9, "命中")));
		DifyRagProperties properties = properties("ds-1");
		properties.getMetaFields().setEnabled(true);

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties)) {
			retriever.retrieve(queryWithMemoryId("0:app1:agent1:user1"));
		}

		ArgumentCaptor<RetrievalModel> captor = ArgumentCaptor.forClass(RetrievalModel.class);
		verify(client).retrieve(eq("ds-1"), anyString(), captor.capture());
		RetrievalModel model = captor.getValue();
		// 基础检索参数不受过滤影响
		assertThat(model.getTopK()).isEqualTo(3);
		KnowledgeMetadataCondition condition = model.getMetadataFilteringConditions();
		assertThat(condition).isNotNull();
		assertThat(condition.getLogical_operator()).isEqualTo("and");
		assertThat(condition.getConditions()).hasSize(4);
		assertThat(condition.getConditions().get(0).getName()).isEqualTo("tenant_id");
		assertThat(condition.getConditions().get(0).getValue()).isEqualTo("0");
		assertThat(condition.getConditions().get(1).getName()).isEqualTo("app_id");
		assertThat(condition.getConditions().get(1).getValue()).isEqualTo("app1");
		assertThat(condition.getConditions().get(2).getName()).isEqualTo("agent_id");
		assertThat(condition.getConditions().get(2).getValue()).isEqualTo("agent1");
		assertThat(condition.getConditions().get(3).getName()).isEqualTo("user_id");
		assertThat(condition.getConditions().get(3).getValue()).isEqualTo("user1");
		assertThat(condition.getConditions().get(3).getComparison_operator()).isEqualTo("is");
		// 请求体序列化字段名为 dify 约定的 snake_case
		String json = JSON.toJSONString(model);
		assertThat(json).contains("metadata_filtering_conditions").contains("logical_operator");
	}

	@Test
	void includeAndFieldNamesAreConfigurable() throws Exception {
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(result(record(0.9, "命中")));
		DifyRagProperties properties = properties("ds-1");
		properties.getMetaFields().setEnabled(true);
		// 只参与两段 + user 段字段名改配
		properties.getMetaFields().setInclude(Arrays.asList("tenant", "user"));
		properties.getMetaFields().setUserField("uid");

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties)) {
			retriever.retrieve(queryWithMemoryId("0:app1:agent1:user1"));
		}

		ArgumentCaptor<RetrievalModel> captor = ArgumentCaptor.forClass(RetrievalModel.class);
		verify(client).retrieve(eq("ds-1"), anyString(), captor.capture());
		List<KnowledgeMetadataCondition.Condition> conditions =
				captor.getValue().getMetadataFilteringConditions().getConditions();
		assertThat(conditions).hasSize(2);
		assertThat(conditions.get(0).getName()).isEqualTo("tenant_id");
		assertThat(conditions.get(0).getValue()).isEqualTo("0");
		assertThat(conditions.get(1).getName()).isEqualTo("uid");
		assertThat(conditions.get(1).getValue()).isEqualTo("user1");
	}

	@Test
	void blankFieldNameSkipsSegment() throws Exception {
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(result(record(0.9, "命中")));
		DifyRagProperties properties = properties("ds-1");
		properties.getMetaFields().setEnabled(true);
		properties.getMetaFields().setTenantField("");

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties)) {
			retriever.retrieve(queryWithMemoryId("0:app1:agent1:user1"));
		}

		ArgumentCaptor<RetrievalModel> captor = ArgumentCaptor.forClass(RetrievalModel.class);
		verify(client).retrieve(eq("ds-1"), anyString(), captor.capture());
		List<KnowledgeMetadataCondition.Condition> conditions =
				captor.getValue().getMetadataFilteringConditions().getConditions();
		assertThat(conditions).hasSize(3);
		assertThat(conditions.get(0).getName()).isEqualTo("app_id");
	}

	@Test
	void filterDisabledByDefault() throws Exception {
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(result(record(0.9, "命中")));

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties("ds-1"))) {
			retriever.retrieve(queryWithMemoryId("0:app1:agent1:user1"));
		}

		ArgumentCaptor<RetrievalModel> captor = ArgumentCaptor.forClass(RetrievalModel.class);
		verify(client).retrieve(eq("ds-1"), anyString(), captor.capture());
		assertThat(captor.getValue().getMetadataFilteringConditions()).isNull();
	}

	@Test
	void nonFourSegmentMemoryIdSkipsFilter() throws Exception {
		when(client.retrieve(eq("ds-1"), anyString(), any())).thenReturn(result(record(0.9, "命中")));
		DifyRagProperties properties = properties("ds-1");
		properties.getMetaFields().setEnabled(true);

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties)) {
			retriever.retrieve(queryWithMemoryId("default"));
		}

		ArgumentCaptor<RetrievalModel> captor = ArgumentCaptor.forClass(RetrievalModel.class);
		verify(client).retrieve(eq("ds-1"), anyString(), captor.capture());
		assertThat(captor.getValue().getMetadataFilteringConditions()).isNull();
	}

	// -------------------------------------------------- 线程池配置

	@Test
	void usesConfiguredThreadNamePrefix() throws Exception {
		List<String> threadNames = Collections.synchronizedList(new ArrayList<String>());
		when(client.retrieve(anyString(), anyString(), any())).thenAnswer(invocation -> {
			threadNames.add(Thread.currentThread().getName());
			return result(record(0.5, "内容"));
		});
		DifyRagProperties properties = properties("ds-1", "ds-2");
		properties.getPool().setThreadNamePrefix("my-retriever");

		try (DifyContentRetriever retriever = new DifyContentRetriever(client, properties)) {
			retriever.retrieve(Query.from("问题"));
		}

		assertThat(threadNames).isNotEmpty();
		for (String threadName : threadNames) {
			assertThat(threadName).startsWith("my-retriever-");
		}
	}

	// -------------------------------------------------- 辅助

	private static DifyRagProperties properties(String... datasetIds) {
		DifyRagProperties properties = new DifyRagProperties();
		properties.setDatasetIds(Arrays.asList(datasetIds));
		return properties;
	}

	private static Query queryWithMemoryId(String memoryId) {
		UserMessage message = UserMessage.from("问题");
		return Query.from("问题", new dev.langchain4j.rag.query.Metadata(message, memoryId,
				Collections.<ChatMessage>emptyList()));
	}

	private static RetrieveResult.Record record(Double score, String content) {
		return record(score, content, Segment.builder().id("seg-" + content).content(content).build());
	}

	private static RetrieveResult.Record record(Double score, String content, Segment segment) {
		RetrieveResult.Record record = new RetrieveResult.Record();
		record.setScore(score);
		record.setSegment(segment);
		return record;
	}

	private static RetrieveResult result(RetrieveResult.Record... records) {
		RetrieveResult result = new RetrieveResult();
		result.setRecords(Arrays.asList(records));
		return result;
	}

	private static List<String> textsOf(List<Content> contents) {
		List<String> texts = new ArrayList<>(contents.size());
		for (Content content : contents) {
			texts.add(content.textSegment().text());
		}
		return texts;
	}

	private static String metadataOf(Content content, String key) {
		return String.valueOf(content.textSegment().metadata().toMap().get(key));
	}
}
