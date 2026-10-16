package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.Assistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.logging.ChatContextLogger;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
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
 * 装配 {@link Assistant}：模型 + 工具 + RAG，经框架的 {@link AssistantBuilder} 链式构建。
 *
 * <p>{@link RetrievalAugmentor} 通过 {@link ObjectProvider} 注入——未配置知识库时它是空的，
 * {@link AssistantBuilder} 便不启用检索增强，样例退化为纯对话。</p>
 *
 * <p>{@code memoryId} 兼任会话记忆键，也决定读取哪份个人资料：{@link AssistantBuilder}
 * 的内置记忆（{@code memory(...)} + {@code memoryStore(...)}）按它隔离多轮上下文，
 * 并包一层 {@code EphemeralChatMemory}：默认只沉淀系统提示、用户原话与上一轮的最终回答——
 * RAG 检索段与工具往返都不留（只在本轮请求里带出）。</p>
 *
 * <p><b>为什么系统提示不走 {@code systemMessageProvider} 的静态注解</b>：provider 每轮产出的
 * 系统提示会被 {@code DefaultAiServices} 写进 memory，而 {@code MessageWindowChatMemory.add}
 * 对 SystemMessage 是「findAny 后替换」语义——memory 里只容得下一条。这里让
 * {@link SystemPromptBuilder#build}（人设 + profile）作为唯一系统提示来源挂到
 * {@code AssistantBuilder#systemMessageProvider} 上，框架自行写进 memory 并按内容去重。</p>
 */
@Configuration
@EnableConfigurationProperties({OllamaProperties.class, OpenAiProperties.class})
public class ChatAssistantConfig {

	/**
	 * 单会话保留的最大消息条数。
	 *
	 * <p>窗口作用在 store 的原始消息上：稳态下只剩系统提示与历史用户原话，但一轮进行中还会
	 * 短暂容纳本轮的工具往返——下一轮开始时被 {@code EphemeralChatMemory} 清掉。</p>
	 */
	private static final int MAX_MEMORY_MESSAGES = 20;

	/** OpenAI 协议路径后缀：Ollama 的兼容端点是 {base-url}/v1/chat/completions */
	private static final String OPENAI_COMPATIBLE_SUFFIX = "/v1";


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

	/**
	 * 流式模型：OpenAI 集成，指向 Ollama 的 OpenAI 兼容端点（{@code base-url}，形如 {@code http://localhost:11434/v1}）。
	 *
	 * <p>配置取自 {@code langchain4j.open-ai.streaming-chat-model}（OpenAI 集成就用 OpenAI 的键）；
	 * 不用 {@code OllamaStreamingChatModel} 的原因：langchain4j 0.35.0 的 ollama 流式实现没有「带工具」的重载，
	 * 调用会抛 {@code Tools are currently not supported by this model}。</p>
	 */
	//@Bean
	public OpenAiStreamingChatModel streamingChatModel(OpenAiProperties properties,
													   ChatContextLogger chatContextLogger) {
		OpenAiProperties.StreamingChatModelProperties p = properties.getStreamingChatModel();
		return OpenAiStreamingChatModel.builder()
				.baseUrl(openAiCompatibleBaseUrl(p.getBaseUrl()))
				.apiKey(p.getApiKey())
				.modelName(p.getModelName())
				.temperature(p.getTemperature())
				.topP(p.getTopP())
				.stop(p.getStop())
				.maxTokens(p.getMaxTokens())
				.seed(p.getSeed())
				.timeout(p.getTimeout())
				.customHeaders(p.getCustomHeaders())
				.logRequests(p.getLogRequests())
				.logResponses(p.getLogResponses())
				.listeners(Collections.singletonList(chatContextLogger))
				.build();
	}

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
				.logRequests(p.getLogRequests())
				.logResponses(p.getLogResponses())
				.customHeaders(p.getCustomHeaders())
				.listeners(Collections.singletonList(chatContextLogger))
				.build();
	}

	/**
	 * 把基础地址补成 OpenAI 兼容端点：{@code http://host:11434} → {@code http://host:11434/v1}；
	 * 已经是 {@code /v1} 结尾的原样返回（OpenAI 客户端会把 {@code /chat/completions} 拼在其后）。
	 */
	static String openAiCompatibleBaseUrl(String baseUrl) {
		if (baseUrl == null || baseUrl.trim().isEmpty()) {
			return baseUrl;
		}
		String trimmed = baseUrl.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed.endsWith(OPENAI_COMPATIBLE_SUFFIX) ? trimmed : trimmed + OPENAI_COMPATIBLE_SUFFIX;
	}

	/**
	 * 经 {@link AssistantBuilder} 装配：模型 / 流式模型 / 系统提示 / 内置记忆 / 工具 / 检索增强
	 * 一条链配齐。记忆由 builder 内置——按 {@code @MemoryId} 实参（即 memoryId）取会话记忆，
	 * {@code MessageWindowChatMemory} 每个会话一个实例，超出窗口的旧消息自动淘汰；外层包
	 * {@code EphemeralChatMemory} 让历史只沉淀系统提示、用户原话与上一轮的最终回答
	 * （RAG 检索段、工具往返都不留；工具往返只在本轮请求里带出）。
	 *
	 * <p>系统提示由 {@link SystemPromptBuilder#build} 每轮生成：人设 + profile。注意 memory 里
	 * 只容得下一条 SystemMessage——框架会把 provider 的产出写进去，所以不要再由别处写第二条，
	 * 否则互相顶掉。</p>
	 */
	@Bean
	public Assistant assistant(ChatLanguageModel chatModel,
							   StreamingChatLanguageModel streamingChatModel,
							   WeatherTools weatherTools,
							   SystemPromptBuilder systemPromptBuilder,
							   ChatMemoryStore chatMemoryStore,
							   ObjectProvider<RetrievalAugmentor> retrievalAugmentor) {

		AssistantBuilder builder = new AssistantBuilder()
				.chatModel(chatModel)
				.streamingModel(streamingChatModel)
				.systemMessageProvider(systemPromptBuilder::build)
				.memory(MAX_MEMORY_MESSAGES)
				.memoryStore(chatMemoryStore)
				.tools(weatherTools);

		// 检索挂在 AiServices 上（LangChain4j 标准用法）；未配置知识库时不启用
		RetrievalAugmentor augmentor = retrievalAugmentor.getIfAvailable();
		if (augmentor != null) {
			builder.retrievalAugmentor(augmentor);
			log.info("知识库检索增强已启用");
		} else {
			log.warn("未检测到知识库配置，本次对话不使用知识库上下文");
		}

		return builder.toAssistant();
	}

	/**
	 * 记忆存储：多会话共享的持久层，进程重启即丢。
	 *
	 * <p>生产环境替换为实现 {@link ChatMemoryStore} 的 Redis / JDBC 存储即可，
	 * {@link #assistant} 的 {@code .memoryStore(...)} 与其他装配都不用动。</p>
	 */
	@Bean
	public ChatMemoryStore chatMemoryStore() {
		return new InMemoryChatMemoryStore();
	}
}
