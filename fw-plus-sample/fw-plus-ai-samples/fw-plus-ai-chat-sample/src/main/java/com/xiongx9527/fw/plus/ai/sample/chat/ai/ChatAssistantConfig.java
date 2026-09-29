package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.Assistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.ChatContextLogger;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * 装配 {@link Assistant}：模型 + 工具 + RAG。
 *
 * <p>{@link RetrievalAugmentor} 通过 {@link ObjectProvider} 注入——未配置知识库时它是空的，
 * {@code AiServices} 便不启用检索增强，样例退化为纯对话。</p>
 *
 * <p>{@code memoryId} 兼任会话记忆键，也决定读取哪份个人资料：{@link #chatMemoryProvider}
 * 按它隔离多轮上下文，并把「人设 + profile」作为唯一一条 SystemMessage 常驻 memory——</p>
 *
 * <p><b>为什么系统提示不走 {@link AiServices#systemMessageProvider}</b>：provider 每轮产出的
 * 系统提示会被 {@code DefaultAiServices} 写进 memory，而 {@code MessageWindowChatMemory.add}
 * 对 SystemMessage 是「findAny 后替换」语义——memory 里只容得下一条。放 profile 的那条会被
 * provider 的下一条顶掉。所以这里让 memory 持有唯一的系统提示，provider 留空。</p>
 */
@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class ChatAssistantConfig {

	/**
	 * 单会话保留的最大消息条数。
	 *
	 * <p>窗口作用在 store 的原始消息上：稳态下只剩系统提示与历史用户原话，但一轮进行中还会
	 * 短暂容纳本轮的工具往返——下一轮开始时被 {@link EphemeralChatMemory} 清掉。</p>
	 */
	private static final int MAX_MEMORY_MESSAGES = 20;

	private static final Logger log = LoggerFactory.getLogger(ChatAssistantConfig.class);

	@Bean
	public WeatherTools weatherTools() {
		return new WeatherTools();
	}

	/**
	 * 同步模型：参数与框架侧 {@code dev.langchain4j.ollama.spring.AutoConfig} 逐项对齐，
	 * 只多出一行 {@code .listeners(...)}——这是本样例自建 Bean 的唯一原因。
	 *
	 * <p>框架侧 ollama 装配已用 {@code fw.plus.ai.langchain4j.ollama.enabled=false} 关掉，
	 * 否则容器内会出现两个 {@link ChatLanguageModel} Bean，{@link #assistant} 按类型注入会报歧义。</p>
	 */
	@Bean
	public OllamaChatModel chatModel(OllamaProperties properties, ChatContextLogger chatContextLogger) {
		OllamaProperties.ModelProperties p = properties.getChatModel();
		return OllamaChatModel.builder()
				.baseUrl(p.getBaseUrl())
				.modelName(p.getModelName())
				.temperature(p.getTemperature())
				.topK(p.getTopK())
				.topP(p.getTopP())
				.repeatPenalty(p.getRepeatPenalty())
				.seed(p.getSeed())
				.numPredict(p.getNumPredict())
				.stop(p.getStop())
				.format(p.getFormat())
				.timeout(p.getTimeout())
				.maxRetries(p.getMaxRetries())
				.customHeaders(p.getCustomHeaders())
				.logRequests(p.getLogRequests())
				.logResponses(p.getLogResponses())
				.listeners(Collections.singletonList(chatContextLogger))
				.build();
	}

	/** 流式模型：同上，供 SSE 链路使用（流式 builder 没有 maxRetries，其余参数一致）。 */
	@Bean
	public OllamaStreamingChatModel streamingChatModel(OllamaProperties properties,
			ChatContextLogger chatContextLogger) {
		OllamaProperties.ModelProperties p = properties.getStreamingChatModel();
		return OllamaStreamingChatModel.builder()
				.baseUrl(p.getBaseUrl())
				.modelName(p.getModelName())
				.temperature(p.getTemperature())
				.topK(p.getTopK())
				.topP(p.getTopP())
				.repeatPenalty(p.getRepeatPenalty())
				.seed(p.getSeed())
				.numPredict(p.getNumPredict())
				.stop(p.getStop())
				.format(p.getFormat())
				.timeout(p.getTimeout())
				.customHeaders(p.getCustomHeaders())
				.logRequests(p.getLogRequests())
				.logResponses(p.getLogResponses())
				.listeners(Collections.singletonList(chatContextLogger))
				.build();
	}

	@Bean
	public Assistant assistant(ChatLanguageModel chatModel,
							   StreamingChatLanguageModel streamingChatModel,
							   WeatherTools weatherTools,
							   SystemPromptBuilder systemPromptBuilder,
							   ChatMemoryProvider chatMemoryProvider,
							   ObjectProvider<RetrievalAugmentor> retrievalAugmentor) {

		// 系统提示由 provider 每轮生成：人设 + profile。
		// 注意 memory 里只容得下一条 SystemMessage——框架会把 provider 的产出写进去，
		// 所以不要再由别处写第二条，否则互相顶掉
		AiServices<Assistant> builder = AiServices.builder(Assistant.class)
				.chatLanguageModel(chatModel)
				.streamingChatLanguageModel(streamingChatModel)
				.chatMemoryProvider(chatMemoryProvider)
				.systemMessageProvider(systemPromptBuilder::build)
				.tools(weatherTools);

		// 检索挂在 AiServices 上（LangChain4j 标准用法）；未配置知识库时不启用
		if (retrievalAugmentor.getIfAvailable() != null) {
			builder.retrievalAugmentor(retrievalAugmentor.getIfAvailable());
			log.info("知识库检索增强已启用");
		} else {
			log.warn("未检测到知识库配置，本次对话不使用知识库上下文");
		}

		return builder.build();
	}

	/**
	 * 记忆存储：多会话共享的持久层，进程重启即丢。
	 *
	 * <p>生产环境替换为实现 {@link ChatMemoryStore} 的 Redis / JDBC 存储即可，
	 * {@link #chatMemoryProvider(ChatMemoryStore)} 与 Assistant 都不用动。</p>
	 */
	@Bean
	public ChatMemoryStore chatMemoryStore() {
		return new InMemoryChatMemoryStore();
	}

	/**
	 * 记忆提供者：AiServices 按 {@code @MemoryId} 实参（即 memoryId）取会话记忆，并缓存在
	 * {@code AiServiceContext.chatMemories} 里——{@code computeIfAbsent} 语义，同一个 memoryId
	 * 只会调用本方法一次，之后复用同一实例。
	 *
	 * <p><b>store 必须是共享 Bean</b>：若在 lambda 里 {@code new InMemoryChatMemoryStore()}，
	 * 多轮上下文会静默失效——这是最常见的坑。</p>
	 *
	 * <p><b>实例跨轮常驻</b>：所以 {@link EphemeralChatMemory} 不持有任何按轮变化的实例状态——
	 * 它的清理与过滤全部基于 store 内容（以「本轮第一条用户消息」为轮次边界），
	 * 同一 memoryId 的并发请求因此不会互相污染暂存。</p>
	 *
	 * <p>返回的 {@link MessageWindowChatMemory} 每个会话一个实例，超出窗口的旧消息自动淘汰；
	 * 外层再包一层 {@link EphemeralChatMemory}，让历史只沉淀系统提示与用户原话——RAG 检索段、
	 * 工具往返、AI 回复都不留（工具往返只在本轮请求里带出）。系统提示不在这里写——由
	 * {@link #assistant} 的 {@code systemMessageProvider} 每轮生成，框架会自行写进 memory
	 * 并按内容去重，不会堆积。</p>
	 */
	@Bean
	public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore chatMemoryStore) {
		return memoryId -> new EphemeralChatMemory(MessageWindowChatMemory.builder()
				.id(memoryId)
				.maxMessages(MAX_MEMORY_MESSAGES)
				.chatMemoryStore(chatMemoryStore)
				.build(), chatMemoryStore);
	}
}
