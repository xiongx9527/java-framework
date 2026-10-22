package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 任务 2.1 + 2.2：抽取策略注册表（名字 / FQCN / 未知名）与 4 个内置策略行为。
 *
 * <p>全部使用进程内 fake（{@link RecordingFactStore} + 匿名 {@link ChatLanguageModel}），
 * 不依赖外部服务；其中 {@code rule} 策略在无任何模型、无存储的前提下也可独立跑通。</p>
 */
class FactExtractorsTest {

	// -------------------------------------------------- 2.1 注册表

	@Test
	void registryResolvesBuiltInNames() {
		FactStore store = new RecordingFactStore();

		assertThat(FactExtractors.resolve("mem0", store, null))
				.isInstanceOf(FactExtractors.Mem0FactExtractor.class);
		assertThat(FactExtractors.resolve("llm", store, null))
				.isInstanceOf(FactExtractors.LlmFactExtractor.class);
		assertThat(FactExtractors.resolve("rule", store, null))
				.isInstanceOf(FactExtractors.RuleFactExtractor.class);
		assertThat(FactExtractors.resolve("hybrid", store, null))
				.isInstanceOf(FactExtractors.HybridFactExtractor.class);
	}

	@Test
	void registryResolvesFqcnByReflection() {
		FactStore store = new RecordingFactStore();

		FactExtractor extractor = FactExtractors.resolve(CustomExtractor.class.getName(), store, null);

		assertThat(extractor).isInstanceOf(CustomExtractor.class);
		extractor.extract("u1", Arrays.asList((ChatMessage) UserMessage.from("你好")));
		assertThat(((CustomExtractor) extractor).calls).isEqualTo(1);
	}

	@Test
	void registryRejectsUnknownName() {
		FactStore store = new RecordingFactStore();

		assertThat(FactExtractors.resolve("bogus", store, null)).isNull();
		assertThatThrownBy(() -> FactExtractors.resolveOrThrow("bogus", store, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("bogus");
	}

	// -------------------------------------------------- 2.2 内置策略

	@Test
	void mem0StrategySubmitsRealDialogue() {
		RecordingFactStore store = new RecordingFactStore();
		FactExtractor extractor = FactExtractors.resolve("mem0", store, null);
		List<ChatMessage> turn = Arrays.asList(UserMessage.from("我叫张三"), AiMessage.from("你好张三"));

		extractor.extract("u1", turn);

		// 抽取式写入：真实对话原文交给 mem0 服务端（infer=true）；不落本地直存
		assertThat(store.dialogues).hasSize(1);
		assertThat(store.dialogues.get(0)).isEqualTo(turn);
		assertThat(store.userIds).containsExactly("u1");
		assertThat(store.facts).isEmpty();
	}

	@Test
	void ruleStrategyExtractsFactsWithoutExternalService() {
		RecordingFactStore store = new RecordingFactStore();
		FactExtractor extractor = FactExtractors.resolve("rule", store, null);

		extractor.extract("u1", Arrays.asList((ChatMessage) UserMessage.from(
				"我喜欢深色主题，我叫张三，手机号 13812345678，邮箱 zhangsan@example.com。")));

		assertThat(store.facts).anyMatch(fact -> fact.contains("喜欢深色主题"));
		assertThat(store.facts).anyMatch(fact -> fact.contains("叫张三"));
		assertThat(store.facts).anyMatch(fact -> fact.contains("13812345678"));
		assertThat(store.facts).anyMatch(fact -> fact.contains("zhangsan@example.com"));
	}

	@Test
	void ruleStrategyExtractsTicketNumber() {
		RecordingFactStore store = new RecordingFactStore();
		FactExtractor extractor = FactExtractors.resolve("rule", store, null);

		extractor.extract("u1", Arrays.asList((ChatMessage) UserMessage.from("帮我查一下订单编号是 A12345678")));

		assertThat(store.facts).anyMatch(fact -> fact.contains("A12345678"));
	}

	@Test
	void ruleStrategySilentWhenNoFact() {
		RecordingFactStore store = new RecordingFactStore();
		FactExtractor extractor = FactExtractors.resolve("rule", store, null);

		extractor.extract("u1", Arrays.asList(
				(UserMessage) UserMessage.from("今天天气怎么样？"),
				AiMessage.from("今天晴，适合出门。")));

		assertThat(store.facts).isEmpty();
		assertThat(store.dialogues).isEmpty();
	}

	@Test
	void llmStrategyParsesFactsFromModelOutput() {
		RecordingFactStore store = new RecordingFactStore();
		ChatLanguageModel model = new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				return Response.from(AiMessage.from("[\"用户喜欢深色主题\", \"用户叫张三\"]"));
			}
		};
		FactExtractor extractor = FactExtractors.resolve("llm", store, model);

		extractor.extract("u1", Arrays.asList((ChatMessage) UserMessage.from("我喜欢深色主题，我叫张三")));

		assertThat(store.facts).containsExactly("用户喜欢深色主题", "用户叫张三");
		assertThat(store.userIds).containsExactly("u1");
	}

	@Test
	void llmStrategySkipsWhenModelMissing() {
		RecordingFactStore store = new RecordingFactStore();
		FactExtractor extractor = FactExtractors.resolve("llm", store, null);

		extractor.extract("u1", Arrays.asList((ChatMessage) UserMessage.from("我喜欢深色主题")));

		assertThat(store.facts).isEmpty();
	}

	@Test
	void llmStrategySkipsUnparseableOutput() {
		RecordingFactStore store = new RecordingFactStore();
		ChatLanguageModel model = new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				return Response.from(AiMessage.from("抱歉，我无法完成"));
			}
		};
		FactExtractor extractor = FactExtractors.resolve("llm", store, model);

		extractor.extract("u1", Arrays.asList((ChatMessage) UserMessage.from("我喜欢深色主题")));

		assertThat(store.facts).isEmpty();
	}

	@Test
	void hybridStrategyFallsBackToRuleWithoutModel() {
		RecordingFactStore store = new RecordingFactStore();
		FactExtractor extractor = FactExtractors.resolve("hybrid", store, null);

		extractor.extract("u1", Arrays.asList((ChatMessage) UserMessage.from("我喜欢深色主题")));

		assertThat(store.facts).anyMatch(fact -> fact.contains("喜欢深色主题"));
	}

	@Test
	void hybridStrategyMergesRuleCandidatesWithModel() {
		RecordingFactStore store = new RecordingFactStore();
		ChatLanguageModel model = new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				return Response.from(AiMessage.from("[\"用户偏好深色主题\"]"));
			}
		};
		FactExtractor extractor = FactExtractors.resolve("hybrid", store, model);

		extractor.extract("u1", Arrays.asList((ChatMessage) UserMessage.from("我喜欢深色主题")));

		assertThat(store.facts).containsExactly("用户偏好深色主题");
	}

	@Test
	void ruleStrategyExtractsSelfIntroductionShi() {
		RecordingFactStore store = new RecordingFactStore();

		FactExtractors.resolve("rule", store, null).extract("u1",
				Arrays.asList((ChatMessage) UserMessage.from("你好，我是李四")));

		assertThat(store.facts).hasSize(1);
		assertThat(store.facts.get(0)).contains("我是李四");
	}

	// -------------------------------------------------- 基础设施

	/** 测试用自定义策略：走 FQCN 反射路径（public 无参构造）。 */
	public static class CustomExtractor implements FactExtractor {

		int calls;

		public CustomExtractor() {
		}

		@Override
		public void extract(String userId, List<ChatMessage> turnMessages) {
			calls++;
		}
	}

	/** 进程内记录桩：只记录调用，不做真实写入。 */
	static final class RecordingFactStore implements FactStore {

		final List<List<ChatMessage>> dialogues = new ArrayList<>();

		final List<String> facts = new ArrayList<>();

		final List<String> userIds = new ArrayList<>();

		@Override
		public void addDialogue(List<ChatMessage> turnMessages, String userId) {
			dialogues.add(new ArrayList<ChatMessage>(turnMessages));
			userIds.add(userId);
		}

		@Override
		public void addFacts(List<String> facts, String userId) {
			this.facts.addAll(facts);
			userIds.add(userId);
		}

		@Override
		public List<String> search(String query, String userId, int topK) {
			return new ArrayList<String>();
		}

		@Override
		public List<String> listAll(String userId, int limit) {
			return new ArrayList<String>();
		}
	}
}
