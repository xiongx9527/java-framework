package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AssistantBuilder} 工具参数错误拦截端到端（mock 模型驱动两轮工具循环）：
 * 缺参不执行工具、拦截文本作为 {@code ToolExecutionResultMessage} 回给 LLM
 * （默认提示反问用户；业务可经 {@code toolArgumentsErrorHandler(...)} 覆盖），
 * 覆盖 {@code tools(...)} 与 {@code toolProvider(...)} 两条注册路径。
 */
class AssistantBuilderToolArgumentsGuardTest {

	static class OrderTool {
		boolean invoked;

		@Tool(name = "order", value = "下单")
		public String order(@P("商品编号") String productId, @P("数量") int quantity) {
			invoked = true;
			return "OK";
		}
	}

	/** 第一轮要求调 order 但只带一个必填参数（缺参），第二轮正常收尾 */
	private ChatLanguageModel modelRoundsWithMissingArgumentRequest() {
		List<String> required = ToolSpecifications.toolSpecificationsFrom(new OrderTool())
				.get(0).parameters().required();
		assertThat(required).hasSize(2);
		ToolExecutionRequest request = ToolExecutionRequest.builder().id("t1").name("order")
				.arguments("{\"" + required.get(0) + "\":\"p1\"}")
				.build();

		ChatLanguageModel model = mock(ChatLanguageModel.class);
		when(model.generate(anyList(), anyList())).thenReturn(
				Response.from(AiMessage.from(request)),
				Response.from(AiMessage.from("请问需要几个？")));
		return model;
	}

	/** 第二轮发给模型的消息中的工具结果文本（即拦截回给 LLM 的内容） */
	@SuppressWarnings("unchecked")
	private List<String> secondRoundToolResultTexts(ChatLanguageModel model) {
		ArgumentCaptor<List> messagesCaptor = ArgumentCaptor.forClass(List.class);
		verify(model, times(2)).generate(messagesCaptor.capture(), anyList());

		List<ChatMessage> secondRound = (List<ChatMessage>) messagesCaptor.getAllValues().get(1);
		List<String> texts = new ArrayList<String>();
		for (ChatMessage message : secondRound) {
			if (message instanceof ToolExecutionResultMessage) {
				texts.add(((ToolExecutionResultMessage) message).text());
			}
		}
		return texts;
	}

	/** 端到端入口：多模态 contents 形态的单条文本（桥接层识别纯文本后转发内层原生管线） */
	private static List<Content> contents(String text) {
		return Collections.singletonList(TextContent.from(text));
	}

	@Test
	void objectToolMissingArgumentIsInterceptedAndDefaultHandlerAsksUser() {
		ChatLanguageModel model = modelRoundsWithMissingArgumentRequest();
		OrderTool tool = new OrderTool();
		Assistant assistant = new AssistantBuilder().chatModel(model).tools(tool).toAssistant();

		String answer = assistant.chat("default", "", contents("下单"));

		assertThat(answer).isEqualTo("请问需要几个？");
		assertThat(tool.invoked).isFalse();

		List<String> texts = secondRoundToolResultTexts(model);
		assertThat(texts).hasSize(1);
		assertThat(texts.get(0)).contains("缺少必要参数").contains("向用户询问");
	}

	@Test
	void providerToolMissingArgumentIsIntercepted() {
		ChatLanguageModel model = modelRoundsWithMissingArgumentRequest();
		AtomicBoolean invoked = new AtomicBoolean(false);
		ToolSpecification specification = ToolSpecifications.toolSpecificationsFrom(new OrderTool()).get(0);
		ToolProvider provider = request -> ToolProviderResult.builder()
				.add(specification, (toolRequest, memoryId) -> {
					invoked.set(true);
					return "OK";
				})
				.build();

		Assistant assistant = new AssistantBuilder().chatModel(model).toolProvider(provider).toAssistant();

		String answer = assistant.chat("default", "", contents("下单"));

		assertThat(answer).isEqualTo("请问需要几个？");
		assertThat(invoked).isFalse();

		List<String> texts = secondRoundToolResultTexts(model);
		assertThat(texts).hasSize(1);
		assertThat(texts.get(0)).contains("缺少必要参数");
	}

	@Test
	void customHandlerTextReplacesDefaultPrompt() {
		ChatLanguageModel model = modelRoundsWithMissingArgumentRequest();
		OrderTool tool = new OrderTool();
		Assistant assistant = new AssistantBuilder().chatModel(model).tools(tool)
				.toolArgumentsErrorHandler((error, request) -> "业务定制：请用户补齐后再调用")
				.toAssistant();

		String answer = assistant.chat("default", "", contents("下单"));

		assertThat(answer).isEqualTo("请问需要几个？");
		assertThat(tool.invoked).isFalse();

		List<String> texts = secondRoundToolResultTexts(model);
		assertThat(texts).hasSize(1);
		assertThat(texts.get(0)).isEqualTo("业务定制：请用户补齐后再调用");
	}

}
