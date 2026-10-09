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
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AssistantBuilder} 工具参数错误处理端到端（mock 模型驱动两轮工具循环）：
 * 缺参不执行工具、拦截文本作为 {@code ToolExecutionResultMessage} 回给 LLM
 * （默认提示反问用户；业务经 {@code toolArgumentsErrorHandler(...)} 覆盖）——
 * 全部经 fork 原生 {@code AiServices.toolArgumentsErrorHandler(...)} 槽位透传，
 * 覆盖 {@code tools(...)} 与 {@code toolProvider(...)} 两条注册路径；
 * 另验证业务异常走原生 {@code toolExecutionErrorHandler(...)} 透传回文本。
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
	private ChatModel modelRoundsWithMissingArgumentRequest() {
		List<String> required = ToolSpecifications.toolSpecificationsFrom(new OrderTool())
				.get(0).parameters().required();
		assertThat(required).hasSize(2);
		ToolExecutionRequest request = ToolExecutionRequest.builder().id("t1").name("order")
				.arguments("{\"" + required.get(0) + "\":\"p1\"}")
				.build();

		ChatModel model = mock(ChatModel.class);
		when(model.chat(any(ChatRequest.class))).thenReturn(
				ChatResponse.builder().aiMessage(AiMessage.from(request)).build(),
				ChatResponse.builder().aiMessage(AiMessage.from("请问需要几个？")).build());
		return model;
	}

	/** 第二轮发给模型的消息中的工具结果文本（即拦截回给 LLM 的内容） */
	private List<String> secondRoundToolResultTexts(ChatModel model) {
		ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
		verify(model, times(2)).chat(requestCaptor.capture());

		List<ChatMessage> secondRound = requestCaptor.getAllValues().get(1).messages();
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
		ChatModel model = modelRoundsWithMissingArgumentRequest();
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
	void providerToolErrorIsInterceptedByNativeArgumentsErrorHandler() {
		// fork 原生不对 toolProvider 路径做缺参检测（executor 由业务完全接管，参数校验是业务自己的职责，
		// dev 的 guardToolProvider 已随 GuardedToolExecutor 下线）；业务 executor 抛出的
		// ToolArgumentsException 仍走透传的 toolArgumentsErrorHandler 回文本
		ChatModel model = modelRoundsWithMissingArgumentRequest();
		AtomicBoolean invoked = new AtomicBoolean(false);
		ToolSpecification specification = ToolSpecifications.toolSpecificationsFrom(new OrderTool()).get(0);
		ToolProvider provider = request -> ToolProviderResult.builder()
				.add(specification, (toolRequest, memoryId) -> {
					invoked.set(true);
					throw new dev.langchain4j.exception.ToolArgumentsException(
							new IllegalArgumentException("Required parameter \"quantity\" of tool \"order\" is missing"));
				})
				.build();

		Assistant assistant = new AssistantBuilder().chatModel(model).toolProvider(provider).toAssistant();

		String answer = assistant.chat("default", "", contents("下单"));

		assertThat(answer).isEqualTo("请问需要几个？");
		assertThat(invoked).isTrue();

		List<String> texts = secondRoundToolResultTexts(model);
		assertThat(texts).hasSize(1);
		assertThat(texts.get(0)).contains("缺少必要参数");
	}

	@Test
	void customHandlerTextReplacesDefaultPrompt() {
		ChatModel model = modelRoundsWithMissingArgumentRequest();
		OrderTool tool = new OrderTool();
		Assistant assistant = new AssistantBuilder().chatModel(model).tools(tool)
				.toolArgumentsErrorHandler((error, context) ->
						ToolErrorHandlerResult.text("业务定制：请用户补齐后再调用"))
				.toAssistant();

		String answer = assistant.chat("default", "", contents("下单"));

		assertThat(answer).isEqualTo("请问需要几个？");
		assertThat(tool.invoked).isFalse();

		List<String> texts = secondRoundToolResultTexts(model);
		assertThat(texts).hasSize(1);
		assertThat(texts.get(0)).isEqualTo("业务定制：请用户补齐后再调用");
	}

	@Test
	void customExecutionErrorHandlerTextReplacesForkDefault() {
		ChatModel model = modelRoundsWithBusinessException();
		BoomTool tool = new BoomTool();
		Assistant assistant = new AssistantBuilder().chatModel(model).tools(tool)
				.toolExecutionErrorHandler((error, context) ->
						ToolErrorHandlerResult.text("业务异常已回传：" + error.getMessage()))
				.toAssistant();

		String answer = assistant.chat("default", "", contents("炸一下"));

		assertThat(answer).isEqualTo("已处理异常");
		assertThat(tool.invoked).isTrue();

		List<String> texts = secondRoundToolResultTexts(model);
		assertThat(texts).hasSize(1);
		assertThat(texts.get(0)).startsWith("业务异常已回传：").contains("库存不足");
	}

	/** 第一轮要求调 boom（参数齐全），工具方法抛业务异常，第二轮正常收尾 */
	private ChatModel modelRoundsWithBusinessException() {
		ToolExecutionRequest request = ToolExecutionRequest.builder().id("t2").name("boom")
				.arguments("{\"x\":\"v\"}")
				.build();

		ChatModel model = mock(ChatModel.class);
		when(model.chat(any(ChatRequest.class))).thenReturn(
				ChatResponse.builder().aiMessage(AiMessage.from(request)).build(),
				ChatResponse.builder().aiMessage(AiMessage.from("已处理异常")).build());
		return model;
	}

	static class BoomTool {
		boolean invoked;

		@Tool(name = "boom", value = "必抛业务异常")
		public String boom(@P("参数") String x) {
			invoked = true;
			throw new IllegalStateException("库存不足");
		}
	}

}