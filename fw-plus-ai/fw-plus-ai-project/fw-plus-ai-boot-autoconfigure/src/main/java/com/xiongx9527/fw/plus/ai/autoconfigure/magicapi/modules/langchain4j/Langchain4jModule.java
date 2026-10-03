package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticWorkflows;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLanguageModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicStreamingLanguageModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.MagicDynamicContentRetriever;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.module.Langchain4jContentRetrieverModule;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.springframework.beans.factory.ObjectProvider;

import java.util.function.Function;

/**
 * LangChain4j 脚本模块入口，三种用法：
 *
 * <ul>
 * <li>{@code langchain4j.assistant()}：返回空 {@link AssistantBuilder}，脚本按参数逐个装配
 * （模型/记忆/工具/检索都由脚本传入）。</li>
 * <li>{@code langchain4j.simpleAssistant()}：由容器注入的全能力 {@code SimpleAssistant} bean
 * （按 {@code fw.plus.magicapi.langchain4j.simple-assistant.*} 配置装配）。</li>
 * <li>单能力便捷方法：{@code chatAssistant()}（仅对话）、{@code languageAssistant()}（仅
 * 文本补全）、{@code streamingLanguageAssistant()}（仅流式补全）、{@code retrievalAssistant()}
 * （仅检索）。</li>
 * </ul>
 *
 * <p>4 个单能力对象在构造方法里获取：MagicDynamic 动态代理包装（module + key，每次调用实时
 * 解析，magic 实例热更新/注销后不持有失效引用）；插件缺席时为 null，方法侧提示。第 5 个
 * 全能力助手由外面的 bean 注入（{@code Langchain4jModuleConfiguration#simpleAssistant}）。</p>
 *
 * <pre>{@code
 * import langchain4j;
 *
 * var assistant = langchain4j.simpleAssistant();
 * var reply = assistant.chat('session-1', '杭州天气怎么样');
 *
 * var chat = langchain4j.chatAssistant().generate('你好');
 * }</pre>
 */
@MagicModule("langchain4j")
public class Langchain4jModule {

	private final SimpleAssistant chatAssistant;

	private final SimpleAssistant languageAssistant;

	private final SimpleAssistant streamingLanguageAssistant;

	private final SimpleAssistant retrievalAssistant;

	/** 全能力助手：由容器注入（{@code Langchain4jModuleConfiguration#simpleAssistant}） */
	private final SimpleAssistant simpleAssistant;

	/** 无依赖构造：仅 {@code assistant()} 可用；其余方法会提示未配置 */
	public Langchain4jModule() {
		this(null, null, null, null, null, null);
	}

	public Langchain4jModule(Langchain4jSimpleAssistantProperties properties,
			ObjectProvider<Langchain4jChatModelModule> chatModelModules,
			ObjectProvider<Langchain4jLanguageModelModule> languageModelModules,
			ObjectProvider<Langchain4jStreamingLanguageModelModule> streamingLanguageModelModules,
			ObjectProvider<Langchain4jContentRetrieverModule> contentRetrieverModules,
			SimpleAssistant simpleAssistant) {
		String key = properties == null ? null : properties.getKey();
		// 4 个单能力对象在构造方法里获取：MagicDynamic 代理包装（module + key，实时解析防实例失效），
		// 插件缺席时为 null（方法侧提示）
		this.chatAssistant = create(
				module(chatModelModules),
				module -> new SimpleAssistant(new MagicDynamicChatModel(module, key)));
		this.languageAssistant = create(
				module(languageModelModules),
				module -> new SimpleAssistant(new MagicDynamicLanguageModel(module, key)));
		this.streamingLanguageAssistant = create(
				module(streamingLanguageModelModules),
				module -> new SimpleAssistant(new MagicDynamicStreamingLanguageModel(module, key)));
		this.retrievalAssistant = create(
				module(contentRetrieverModules),
				module -> new SimpleAssistant(DefaultRetrievalAugmentor.builder()
						.contentRetriever(new MagicDynamicContentRetriever(module, key))
						.build()));
		// 第 5 个：全能力助手由外面的 bean 注入（Langchain4jModuleConfiguration#simpleAssistant）
		this.simpleAssistant = simpleAssistant;
	}

	private static <M> M module(ObjectProvider<M> modules) {
		return modules == null ? null : modules.getIfAvailable();
	}

	private static <M, R> R create(M module, Function<M, R> factory) {
		return module == null ? null : factory.apply(module);
	}

	/** 快速构建助手：链式配置模型/记忆/系统提示/工具/检索，最后 {@code build()} 得到 {@code SimpleAssistant}。 */
	public AssistantBuilder assistant() {
		return new AssistantBuilder();
	}

	/** 仅对话助手（构造时创建）：直连 {@code generate(...)}，无记忆、无流式。 */
	public SimpleAssistant chatAssistant() {
		return require(chatAssistant, "chat-model");
	}

	/** 仅文本补全助手（构造时创建）：{@code generateText(...)} 直连补全。 */
	public SimpleAssistant languageAssistant() {
		return require(languageAssistant, "language-model");
	}

	/** 仅流式补全助手（构造时创建）：{@code generateText(..., onToken)} / SSE 推流。 */
	public SimpleAssistant streamingLanguageAssistant() {
		return require(streamingLanguageAssistant, "streaming-language-model");
	}

	/** 仅检索助手（构造时创建）：{@code retrieve(...)} 不调用对话大模型。 */
	public SimpleAssistant retrievalAssistant() {
		return require(retrievalAssistant, "content-retriever");
	}

	/**
	 * agentic 编排入口：脚本经本方法构建 sequence / loop / conditional / parallel 编排，
	 * 或经 {@code agenticToolProvider(...)} 把编排包装为工具挂入助手
	 * （依赖 langchain4j-agentic / langgraph4j，未引入时调用将抛出类缺失错误）。
	 */
	public AgenticWorkflows agentic() {
		return com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticWorkflows.instance();
	}

	/** 全能力助手（容器注入）：chat / stream / 补全 / 检索 / 记忆 / 工具。 */
	public SimpleAssistant simpleAssistant() {
		return require(simpleAssistant, "simple-assistant（请配置 fw.plus.magicapi.langchain4j.simple-assistant.enabled=true "
				+ "及 keys.<类型>）");
	}

	private SimpleAssistant require(SimpleAssistant assistant, String resourceType) {
		if (assistant == null) {
			throw new IllegalArgumentException("当前应用未装配 langchain4j-" + resourceType
					+ " 相关资源，无法提供该助手");
		}
		return assistant;
	}
}
