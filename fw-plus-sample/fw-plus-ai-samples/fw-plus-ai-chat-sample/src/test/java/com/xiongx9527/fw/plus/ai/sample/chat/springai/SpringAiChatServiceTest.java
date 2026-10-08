package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.SystemPromptBuilder;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.UserProfileProvider;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.WeatherTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link SpringAiChatService} 单元测试：不连模型，用桩 {@link ChatModel} 驱动真实的
 * ChatClient / MessageChatMemoryAdvisor / RetrievalAugmentationAdvisor / SSE 推流逻辑。
 *
 * <p>覆盖：系统提示拼装、sessionId 记忆隔离、知识库 RAG（原生 Advisor 形态：
 * 注入本轮 user message、不落记忆、空检索保持原文、流式同样生效）、流式增量、
 * SSE 事件序列（start/token/done）——与 LangChain4j 链路能力对齐的五个面。</p>
 */
class SpringAiChatServiceTest {

	private StubChatModel stubModel;
	private WeatherTools weatherTools;
	private SpringAiChatService service;

	@BeforeEach
	void setUp() {
		stubModel = new StubChatModel();
		weatherTools = new WeatherTools();
		service = newService();
	}

	/** 组装被测服务：真实 ChatClient + 窗口记忆 Advisor，桩模型负责应答（无知识库）。 */
	private SpringAiChatService newService() {
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
		ChatClient chatClient = ChatClient.builder(stubModel)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.build();
		return new SpringAiChatService(chatClient, weatherTools,
				new SystemPromptBuilder(new UserProfileProvider()));
	}

	/**
	 * 组装带原生 RAG 管线的被测服务：记忆 Advisor 在前（默认 order 极先执行，
	 * 先落盘原始 user message），检索 Advisor 在后——与 {@code SpringAiChatConfig}
	 * 的装配顺序一致。
	 */
	private SpringAiChatService newServiceWithRetrieval(DocumentRetriever retriever) {
		ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
		ChatClient chatClient = ChatClient.builder(stubModel)
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
				.defaultAdvisors(RetrievalAugmentationAdvisor.builder()
						.documentRetriever(retriever)
						// 与 SpringAiChatConfig 装配一致：空检索保持 user message 原文
						.queryAugmenter(ContextualQueryAugmenter.builder()
								.allowEmptyContext(Boolean.TRUE)
								.build())
						.build())
				.build();
		return new SpringAiChatService(chatClient, weatherTools,
				new SystemPromptBuilder(new UserProfileProvider()));
	}

	@Test
	void chatReturnsModelReply() {
		stubModel.reply = "杭州夏天更凉快一些。";

		String reply = service.chat("s-1", "杭州和北京哪个更适合夏天出行？");

		assertEquals("杭州夏天更凉快一些。", reply);
	}

	@Test
	void chatBuildsSystemPromptWithPersonaAndUserMessage() {
		stubModel.reply = "好的";

		service.chat("s-1", "杭州有什么景点");

		Prompt prompt = stubModel.prompts.get(0);
		Message first = prompt.getInstructions().get(0);
		assertTrue(first.getText().contains("fw-plus 内置智能助手"), "系统提示应含人设: " + first.getText());
		Message last = prompt.getInstructions().get(prompt.getInstructions().size() - 1);
		assertEquals("杭州有什么景点", last.getText(), "最后一条应是本轮用户消息");
	}

	@Test
	void memoryIsolatedBySessionId() {
		stubModel.reply = "你好，小明。";
		service.chat("s-1", "我叫小明");

		// 跨 session：s-2 看不到 s-1 的历史
		stubModel.reply = "你还没有做过自我介绍。";
		service.chat("s-2", "我叫什么名字");
		String otherSession = instructions(stubModel.prompts.get(1));
		assertFalse(otherSession.contains("我叫小明"), "s-2 不应包含 s-1 的历史: " + otherSession);

		// 同 session：s-1 能延续上一轮
		stubModel.reply = "你叫小明。";
		service.chat("s-1", "我叫什么名字");
		String sameSession = instructions(stubModel.prompts.get(2));
		assertTrue(sameSession.contains("我叫小明"), "s-1 应包含自己的历史: " + sameSession);
	}

	@Test
	void retrievalAdvisorInjectsDocumentsIntoUserMessage() {
		service = newServiceWithRetrieval(query -> Collections.singletonList(
				new Document("西湖十景是杭州的代表性景点。", Collections.singletonMap("title", "西湖"))));
		stubModel.reply = "好的";

		service.chat("s-1", "杭州有什么景点");

		Prompt prompt = stubModel.prompts.get(0);
		Message system = prompt.getInstructions().get(0);
		Message user = prompt.getInstructions().get(prompt.getInstructions().size() - 1);
		assertTrue(user.getText().contains("西湖十景是杭州的代表性景点。"),
				"检索结果应注入本轮 user message: " + user.getText());
		assertTrue(user.getText().contains("杭州有什么景点"), "注入应保留原问题: " + user.getText());
		assertFalse(system.getText().contains("西湖十景"),
				"检索结果不应进系统提示（由 Advisor 注入 user message）: " + system.getText());
	}

	@Test
	void emptyRetrievalKeepsOriginalUserMessage() {
		service = newServiceWithRetrieval(query -> Collections.emptyList());
		stubModel.reply = "好的";

		service.chat("s-1", "杭州有什么景点");

		Prompt prompt = stubModel.prompts.get(0);
		Message user = prompt.getInstructions().get(prompt.getInstructions().size() - 1);
		assertEquals("杭州有什么景点", user.getText(), "空检索结果时 user message 应保持原文");
	}

	@Test
	void retrievalContextNotPersistedToMemory() {
		// 检索器只在第一轮（问题含"景点"）命中，第二轮返回空——便于断言历史是否被污染
		service = newServiceWithRetrieval(query ->
				query.text() != null && query.text().contains("景点")
						? Collections.singletonList(new Document("西湖十景是杭州的代表性景点。"))
						: Collections.emptyList());
		stubModel.reply = "杭州有西湖十景。";
		service.chat("s-1", "杭州有什么景点");

		stubModel.reply = "上一轮问的是杭州景点。";
		service.chat("s-1", "再问一次");

		String secondPrompt = instructions(stubModel.prompts.get(1));
		assertFalse(secondPrompt.contains("西湖十景是杭州的代表性景点。"),
				"检索上下文不应沉淀进会话记忆: " + secondPrompt);
		assertTrue(secondPrompt.contains("杭州有什么景点"), "原始 user message 应进记忆: " + secondPrompt);
	}

	@Test
	void streamAlsoRetrievesDocuments() {
		service = newServiceWithRetrieval(query -> Collections.singletonList(
				new Document("杭州夏季均温 28℃。")));
		stubModel.streamTexts = Arrays.asList("杭州", "凉快");

		List<String> tokens = service.streamTokens("s-1", "杭州天气").collectList().block();

		assertEquals(Arrays.asList("杭州", "凉快"), tokens);
		String prompt = instructions(stubModel.prompts.get(0));
		assertTrue(prompt.contains("杭州夏季均温 28℃。"), "流式链路同样应执行检索注入: " + prompt);
	}

	@Test
	void streamTokensYieldsIncrementalText() {
		stubModel.streamTexts = Arrays.asList("杭州", "夏天", "凉快");

		List<String> tokens = service.streamTokens("s-1", "介绍杭州").collectList().block();

		assertEquals(Arrays.asList("杭州", "夏天", "凉快"), tokens);
	}

	@Test
	void streamToSseEmitsStartTokenDoneEvents() throws Exception {
		stubModel.streamTexts = Arrays.asList("杭州", "凉快");
		SpringAiChatController controller = new SpringAiChatController(service, weatherTools);
		// standalone MockMvc 默认 StringHttpMessageConverter 是 ISO-8859-1（Spring MVC 默认），
		// 中文 SSE 会变 "??"；Boot 运行时按 server.servlet.encoding.charset=UTF-8 构造该转换器，
		// 这里注入同等配置以对齐真实行为。String 转换器必须放首位，否则 SSE 事件的 data
		// （裸 String、mediaType 为 null）会先被 Jackson 接住、被包成带引号的 JSON 字符串
		MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
				.setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
						new MappingJackson2HttpMessageConverter())
				.build();

		MvcResult result = mvc.perform(post("/api/chat/spring-ai/stream")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"介绍杭州\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
				.andReturn();

		String body = readBody(result);
		assertTrue(body.contains("event:start"), "应含 start 事件: " + body);
		assertTrue(body.contains("event:token"), "应含 token 事件: " + body);
		assertTrue(body.contains("event:done"), "应含 done 事件: " + body);
		assertTrue(body.contains("data:杭州凉快"), "done data 应为完整回复: " + body);
	}

	/**
	 * SSE 响应体读取：emitter 推流在 perform 返回后异步写入 Mock response，
	 * 轮询等待终态事件（done/error）出现；返回 UTF-8 解码的完整内容。
	 */
	private static String readBody(MvcResult result) throws Exception {
		long deadline = System.currentTimeMillis() + 5000;
		String body = "";
		while (System.currentTimeMillis() < deadline) {
			body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
			if (body.contains("event:done") || body.contains("event:error")) {
				break;
			}
			Thread.sleep(20);
		}
		return body;
	}

	private static String instructions(Prompt prompt) {
		return prompt.getInstructions().stream()
				.map(Message::getText)
				.collect(Collectors.joining("\n"));
	}

	/**
	 * 可编程的 ChatModel 桩：记录收到的 Prompt，同步返回预设回复或预设 token 流。
	 * {@code stream(Prompt)} 必须覆盖——接口默认实现抛 UnsupportedOperationException。
	 */
	private static class StubChatModel implements ChatModel {

		private final List<Prompt> prompts = new ArrayList<>();
		private String reply = "默认回复";
		private List<String> streamTexts = Arrays.asList("默认", "回复");

		@Override
		public ChatResponse call(Prompt prompt) {
			prompts.add(prompt);
			return response(reply);
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			prompts.add(prompt);
			return Flux.fromIterable(streamTexts).map(StubChatModel::response);
		}

		private static ChatResponse response(String text) {
			return new ChatResponse(Collections.singletonList(new Generation(new AssistantMessage(text))));
		}
	}
}
