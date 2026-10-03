package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LLM 节点验证：模板插值（缺参置空）、system/user 消息组装、chat 插件缺席时的可操作错误、
 * 编排所需的访问器。
 */
class MagicLlmAgentTest {

	@Test
	void shouldInterpolateTemplateAndCallModel() {
		Langchain4jAgenticScriptInfo info = info();
		info.setSystemMessage("你是意图识别器");
		info.setScript("问题: {{question}}\n历史: {{history}}");
		RecordingChatModel model = new RecordingChatModel("DATA_QUERY");
		MagicLlmAgent agent = agent(info, model);

		Object result = agent.invoke(Map.of("question", "上月销售额"));

		assertThat(result).isEqualTo("DATA_QUERY");
		// 模板渲染：question 替换，history 缺参置空串
		List<ChatMessage> messages = model.requests.get(0);
		assertThat(messages).hasSize(2);
		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
		assertThat(messages.get(0).toString()).contains("你是意图识别器");
		assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
		assertThat(messages.get(1).toString()).contains("问题: 上月销售额").contains("历史: ");
	}

	@Test
	void shouldReplaceMissingPlaceholderWithEmptyAndOmitSystemWhenBlank() {
		Langchain4jAgenticScriptInfo info = info();
		info.setScript("问题: {{question}}，范围: {{scope}}");
		RecordingChatModel model = new RecordingChatModel("ok");
		MagicLlmAgent agent = agent(info, model);

		agent.invoke(Map.of("question", "你好"));

		List<ChatMessage> messages = model.requests.get(0);
		// systemMessage 未配置：只发 user message
		assertThat(messages).hasSize(1);
		assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
		// 未传入的占位符 scope 置空串
		assertThat(messages.get(0).toString()).contains("问题: 你好，范围: ");
	}

	@Test
	void shouldThrowOperableErrorWhenChatPluginAbsent() {
		MagicLlmAgent agent = new MagicLlmAgent(info(), null);
		assertThatThrownBy(() -> agent.invoke(Map.of("question", "你好")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("langchain4j-chat")
				.hasMessageContaining("/intent-classifier");
	}

	@Test
	void shouldExposeAccessorsForOrchestration() {
		MagicLlmAgent agent = agent(info(), new RecordingChatModel());
		assertThat(agent.getPath()).isEqualTo("/intent-classifier");
		// name 未配置时缺省取 path 原样（与脚本节点一致）
		assertThat(agent.getName()).isEqualTo("/intent-classifier");
		assertThat(agent.getType()).isEqualTo("llm");
		assertThat(agent.getOutputKey()).isEqualTo("intentResult");
		assertThat(agent.getInputKeys()).containsExactly("question", "history");
	}

	/** info 定义：与控制台资源一致的 llm 形态样例 */
	private static Langchain4jAgenticScriptInfo info() {
		Langchain4jAgenticScriptInfo info = new Langchain4jAgenticScriptInfo();
		info.setType("llm");
		info.setPath("/intent-classifier");
		info.setInputKeys(Arrays.asList("question", "history"));
		info.setOutputKey("intentResult");
		info.setModelKey("deepseek");
		return info;
	}

	/** 用注入的 fake 模型替代 MagicDynamicChatModel 解析（invoke 缝隙：resolveModel 可覆写） */
	private static MagicLlmAgent agent(Langchain4jAgenticScriptInfo info, ChatModel model) {
		return new MagicLlmAgent(info, org.mockito.Mockito.mock(
				com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule.class)) {
			@Override
			protected ChatModel resolveModel() {
				return model;
			}
		};
	}

	/** 记录请求消息的 fake 模型（1.x 须覆写 doChat） */
	private static class RecordingChatModel implements ChatModel {

		private final List<List<ChatMessage>> requests = new ArrayList<>();

		private final List<String> replies;

		private RecordingChatModel(String... replies) {
			this.replies = Arrays.asList(replies.length == 0 ? new String[]{""} : replies);
		}

		@Override
		public ChatResponse doChat(ChatRequest request) {
			requests.add(new ArrayList<>(request.messages()));
			int index = Math.min(requests.size() - 1, replies.size() - 1);
			return ChatResponse.builder().aiMessage(AiMessage.from(replies.get(index))).build();
		}
	}
}
