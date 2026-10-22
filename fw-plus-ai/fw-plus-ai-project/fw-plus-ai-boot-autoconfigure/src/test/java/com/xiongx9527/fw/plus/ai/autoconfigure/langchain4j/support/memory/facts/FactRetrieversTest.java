package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 任务 3.1 + 3.2：召回策略注册表与 4 个内置策略（semantic / keyword / hybrid / rerank）。
 */
class FactRetrieversTest {

	private static final String USER = "u1";

	// -------------------------------------------------- 注册表（3.1）

	@Test
	void registryResolvesBuiltInNames() {
		RecordingFactStore store = new RecordingFactStore();

		assertThat(FactRetrievers.resolve("semantic", store, null))
				.isInstanceOf(FactRetrievers.SemanticFactRetriever.class);
		assertThat(FactRetrievers.resolve("KEYWORD", store, null))
				.isInstanceOf(FactRetrievers.KeywordFactRetriever.class);
		assertThat(FactRetrievers.resolve("hybrid", store, null))
				.isInstanceOf(FactRetrievers.HybridFactRetriever.class);
		assertThat(FactRetrievers.resolve("rerank", store, null))
				.isInstanceOf(FactRetrievers.RerankFactRetriever.class);
	}

	@Test
	void registryReturnsNullAndResolveOrThrowFailsForUnknown() {
		RecordingFactStore store = new RecordingFactStore();

		assertThat(FactRetrievers.resolve("bogus", store, null)).isNull();
		assertThatThrownBy(() -> FactRetrievers.resolveOrThrow("bogus", store, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("bogus");
	}

	@Test
	void registryResolvesFqcnByReflection() {
		RecordingFactStore store = new RecordingFactStore();

		assertThat(FactRetrievers.resolve(CustomRetriever.class.getName(), store, null))
				.isInstanceOf(CustomRetriever.class);
	}

	// -------------------------------------------------- semantic（3.2）

	@Test
	void semanticDelegatesToStoreAndHonoursTopK() {
		RecordingFactStore store = new RecordingFactStore();
		store.searchResults = Arrays.asList("A1", "A2", "A3", "A4", "A5");
		FactRetriever retriever = FactRetrievers.resolve("semantic", store, null);

		List<String> facts = retriever.retrieve("查询", USER, 2);

		assertThat(facts).containsExactly("A1", "A2");
		assertThat(store.lastSearchUserId).isEqualTo(USER);
		assertThat(store.lastSearchQuery).isEqualTo("查询");
		assertThat(store.lastSearchTopK).isEqualTo(2);
	}

	@Test
	void semanticSkipsBlankAndNullEntries() {
		RecordingFactStore store = new RecordingFactStore();
		store.searchResults = Arrays.asList("A1", null, "  ", "A2");
		FactRetriever retriever = FactRetrievers.resolve("semantic", store, null);

		assertThat(retriever.retrieve("查询", USER, 5)).containsExactly("A1", "A2");
	}

	// -------------------------------------------------- keyword（3.2）

	@Test
	void keywordScoresByQueryTokenCoverage() {
		RecordingFactStore store = new RecordingFactStore();
		store.allFacts = Arrays.asList("用户喜欢深色主题", "用户住在上海", "深色主题背景已设置");
		FactRetriever retriever = FactRetrievers.resolve("keyword", store, null);

		List<String> facts = retriever.retrieve("深色主题", USER, 5);

		assertThat(facts).containsExactly("用户喜欢深色主题", "深色主题背景已设置");
		assertThat(store.lastListLimit).as("关键词检索取样本上限").isEqualTo(200);
	}

	@Test
	void keywordRespectsTopK() {
		RecordingFactStore store = new RecordingFactStore();
		store.allFacts = Arrays.asList("用户喜欢深色主题", "深色主题背景已设置");
		FactRetriever retriever = FactRetrievers.resolve("keyword", store, null);

		assertThat(retriever.retrieve("深色主题", USER, 1)).containsExactly("用户喜欢深色主题");
	}

	@Test
	void keywordMatchesAsciiCaseInsensitively() {
		RecordingFactStore store = new RecordingFactStore();
		store.allFacts = Arrays.asList("python 教程入门", "用户住在上海");
		FactRetriever retriever = FactRetrievers.resolve("keyword", store, null);

		assertThat(retriever.retrieve("PYTHON 教程", USER, 5)).containsExactly("python 教程入门");
	}

	@Test
	void guardsShortCircuitWithoutStoreCalls() {
		RecordingFactStore store = new RecordingFactStore();
		FactRetriever semantic = FactRetrievers.resolve("semantic", store, null);
		FactRetriever keyword = FactRetrievers.resolve("keyword", store, null);

		assertThat(semantic.retrieve("  ", USER, 5)).isEmpty();
		assertThat(semantic.retrieve("查询", null, 5)).isEmpty();
		assertThat(semantic.retrieve("查询", USER, 0)).isEmpty();
		assertThat(keyword.retrieve("查询", USER, 0)).isEmpty();
		assertThat(keyword.retrieve("。。", USER, 5)).as("无可用 token").isEmpty();
		assertThat(store.lastSearchQuery).as("守卫命中不调 store.search").isNull();
		assertThat(store.lastListLimit).as("守卫命中不调 store.listAll").isZero();
	}

	// -------------------------------------------------- hybrid（3.2）

	@Test
	void hybridFusesSemanticAndKeywordWithRrf() {
		RecordingFactStore store = new RecordingFactStore();
		store.searchResults = Arrays.asList("用户喜欢深色主题", "用户叫张三");
		store.allFacts = Arrays.asList("用户喜欢深色主题", "深色主题背景已设置");
		FactRetriever retriever = FactRetrievers.resolve("hybrid", store, null);

		List<String> facts = retriever.retrieve("深色主题", USER, 3);

		assertThat(facts).containsExactly("用户喜欢深色主题", "用户叫张三", "深色主题背景已设置");
		assertThat(store.lastSearchTopK).as("semantic 取 2K").isEqualTo(6);
	}

	// -------------------------------------------------- rerank（3.2）

	@Test
	void rerankFallsBackToLocalOverlapWithoutModel() {
		RecordingFactStore store = new RecordingFactStore();
		store.searchResults = Arrays.asList("用户住在上海", "用户喜欢深色主题");
		FactRetriever retriever = FactRetrievers.resolve("rerank", store, null);

		List<String> facts = retriever.retrieve("深色主题", USER, 2);

		assertThat(facts).containsExactly("用户喜欢深色主题", "用户住在上海");
		assertThat(store.lastSearchTopK).as("semantic 取 4K").isEqualTo(8);
	}

	@Test
	void rerankUsesScoringModelWhenConfigured() {
		RecordingFactStore store = new RecordingFactStore();
		store.searchResults = Arrays.asList("用户住在上海", "用户喜欢深色主题");
		ScoringModel model = new ScoringModel() {
			@Override
			public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
				List<Double> scores = new ArrayList<Double>();
				for (TextSegment segment : segments) {
					scores.add(segment.text().contains("上海") ? 0.9 : 0.1);
				}
				return Response.from(scores);
			}
		};
		FactRetriever retriever = FactRetrievers.resolve("rerank", store, model);

		List<String> facts = retriever.retrieve("深色主题", USER, 2);

		assertThat(facts).containsExactly("用户住在上海", "用户喜欢深色主题");
	}

	@Test
	void rerankFallsBackToOverlapWhenModelFails() {
		RecordingFactStore store = new RecordingFactStore();
		store.searchResults = Arrays.asList("用户住在上海", "用户喜欢深色主题");
		ScoringModel model = new ScoringModel() {
			@Override
			public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
				throw new IllegalStateException("rerank 服务不可用");
			}
		};
		FactRetriever retriever = FactRetrievers.resolve("rerank", store, model);

		List<String> facts = retriever.retrieve("深色主题", USER, 2);

		assertThat(facts).as("模型失败回退本地重叠").containsExactly("用户喜欢深色主题", "用户住在上海");
	}

	// -------------------------------------------------- 基础设施

	/** 测试用自定义策略：走 FQCN 反射路径（public 无参构造）。 */
	public static class CustomRetriever implements FactRetriever {

		public CustomRetriever() {
		}

		@Override
		public List<String> retrieve(String query, String userId, int topK) {
			return new ArrayList<String>();
		}
	}

	/** 进程内记录桩：记录检索调用参数，返回预置结果。 */
	static final class RecordingFactStore implements FactStore {

		List<String> allFacts = new ArrayList<String>();

		List<String> searchResults = new ArrayList<String>();

		String lastSearchQuery;

		String lastSearchUserId;

		int lastSearchTopK;

		String lastListUserId;

		int lastListLimit;

		@Override
		public void addDialogue(List<ChatMessage> turnMessages, String userId) {
		}

		@Override
		public void addFacts(List<String> facts, String userId) {
		}

		@Override
		public List<String> search(String query, String userId, int topK) {
			lastSearchQuery = query;
			lastSearchUserId = userId;
			lastSearchTopK = topK;
			return new ArrayList<String>(searchResults);
		}

		@Override
		public List<String> listAll(String userId, int limit) {
			lastListUserId = userId;
			lastListLimit = limit;
			return new ArrayList<String>(allFacts);
		}
	}
}
