package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag;

import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.utils.DatasetBuilder;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 对真实 dify 知识库检索接口的联调（端点 / 认证 / 必填字段 / 元数据过滤 / 多库 / 降级全链路）。
 *
 * <p>通过 {@code DIFY_BASE_URL} + {@code DIFY_API_KEY} + {@code DIFY_DATASET_IDS}（逗号分隔，
 * 系统属性或环境变量）提供时执行，否则整体跳过。</p>
 *
 * <p>{@code realServerSingleDatasetRetrieval} 隐式验证 {@link DifyRagProperties#toRetrievalModel()}
 * 对 dify 必填字段（search_method / reranking_enable / score_threshold_enabled）的默认值修复——
 * 字段缺失时 dify 返回 400，客户端降级为空列表，该用例的非空断言会失败。</p>
 */
class DifyContentRetrieverRealServerTest {

	@Test
	void realServerSingleDatasetRetrieval() {
		Env env = requireEnv();
		DifyRagProperties properties = properties(env, env.datasetIds[0]);
		try (DifyContentRetriever retriever = new DifyContentRetriever(newClient(properties), properties)) {
			List<Content> contents = retriever.retrieve(Query.from("测试"));
			assertThat(contents).isNotEmpty();
			for (Content content : contents) {
				assertThat(content.textSegment().text()).isNotBlank();
			}
		}
	}

	@Test
	void realServerMultiDatasetFanOut() {
		Env env = requireEnv();
		assumeTrue(env.datasetIds.length >= 2, "DIFY_DATASET_IDS 需至少两个 ID 才能验证多库扇出");
		DifyRagProperties properties = properties(env, env.datasetIds);
		try (DifyContentRetriever retriever = new DifyContentRetriever(newClient(properties), properties)) {
			// 多库并行：空库返回空被跳过，结果来自有内容的库；任何单库失败只 WARN 不抛
			List<Content> contents = retriever.retrieve(Query.from("测试"));
			assertThat(contents).isNotEmpty();
		}
	}

	@Test
	void realServerMetadataFilterYieldsEmptyWhenUnlabeled() {
		Env env = requireEnv();
		DifyRagProperties properties = properties(env, env.datasetIds);
		properties.getMetaFields().setEnabled(true);
		try (DifyContentRetriever retriever = new DifyContentRetriever(newClient(properties), properties)) {
			// 四段 memId → metadata_filtering_conditions 真实下发；
			// 文档未打元数据标签时过滤命中为空（降级路径：200 + 空列表，不抛异常）
			List<Content> contents = retriever.retrieve(queryWithMemoryId("it:app:agent:user"));
			assertThat(contents).isEmpty();
		}
	}

	@Test
	void realServerFailureDegradesToEmpty() {
		Env env = requireEnv();
		DifyRagProperties properties = properties(env, env.datasetIds);
		properties.setApiKey("dataset-invalid-" + env.apiKey);
		try (DifyContentRetriever retriever = new DifyContentRetriever(newClient(properties), properties)) {
			// 认证失败 → 单库失败 WARN 跳过 → 降级空列表，不向外抛
			assertThat(retriever.retrieve(Query.from("测试"))).isEmpty();
		}
	}

	// -------------------------------------------------- 辅助

	private static IDatasetClient newClient(DifyRagProperties properties) {
		return DatasetBuilder.builder(properties.toBaseUrl(), properties.getApiKey()).build();
	}

	private static DifyRagProperties properties(Env env, String... datasetIds) {
		DifyRagProperties properties = new DifyRagProperties();
		properties.setBaseUrl(env.baseUrl);
		properties.setApiKey(env.apiKey);
		properties.setDatasetIds(Arrays.asList(datasetIds));
		return properties;
	}

	private static Query queryWithMemoryId(String memoryId) {
		UserMessage message = UserMessage.from("测试");
		return Query.from("测试", new dev.langchain4j.rag.query.Metadata(message, memoryId,
				Collections.<ChatMessage>emptyList()));
	}

	private static Env requireEnv() {
		String baseUrl = config("DIFY_BASE_URL");
		String apiKey = config("DIFY_API_KEY");
		String datasetIds = config("DIFY_DATASET_IDS");
		assumeTrue(baseUrl != null && !baseUrl.isEmpty(), "未提供 DIFY_BASE_URL，跳过真实 dify 联调");
		assumeTrue(apiKey != null && !apiKey.isEmpty(), "未提供 DIFY_API_KEY，跳过真实 dify 联调");
		assumeTrue(datasetIds != null && !datasetIds.isEmpty(), "未提供 DIFY_DATASET_IDS，跳过真实 dify 联调");
		return new Env(baseUrl, apiKey, datasetIds.split("\\s*,\\s*"));
	}

	private static String config(String name) {
		String value = System.getProperty(name);
		return value != null ? value : System.getenv(name);
	}

	private static final class Env {

		private final String baseUrl;

		private final String apiKey;

		private final String[] datasetIds;

		private Env(String baseUrl, String apiKey, String[] datasetIds) {
			this.baseUrl = baseUrl;
			this.apiKey = apiKey;
			this.datasetIds = datasetIds;
		}
	}
}
