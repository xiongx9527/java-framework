package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLanguageModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicStreamingChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicStreamingLanguageModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingLanguageModelModule;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
 * 文本补全）、{@code streamingLanguageAssistant()}（仅流式补全）。</li>
 * </ul>
 *
 * <p>3 个单能力对象在构造方法里获取：MagicDynamic 动态代理包装（module + key，每次调用实时
 * 解析，magic 实例热更新/注销后不持有失效引用）；插件缺席时为 null，方法侧提示。第 4 个
 * 全能力助手由外面的 bean 注入（{@code Langchain4jModuleConfiguration#simpleAssistant}）。</p>
 *
 * <pre>{@code
 * import langchain4j;
 *
 * var assistant = langchain4j.simpleAssistant();
 * var reply = assistant.chat('session-1', '杭州天气怎么样');
 *
 * var chat = langchain4j.chatAssistant().generate('你好');
 *
 * // 自建助手复用容器同一份记忆存储（mem0 等），跨脚本调用保留会话历史
 * var custom = langchain4j.assistant()
 *         .chatModel(langchain4jChatModel['deepseek'])
 *         .memoryStore(langchain4j.memoryStore())
 *         .build();
 * }</pre>
 */
@MagicModule("langchain4j")
public class Langchain4jModule {

	private final SimpleAssistant chatAssistant;

	private final SimpleAssistant languageAssistant;

	private final SimpleAssistant streamingLanguageAssistant;

	/** 全能力助手：由容器注入（{@code Langchain4jModuleConfiguration#simpleAssistant}） */
	private final SimpleAssistant simpleAssistant;

	/** 会话记忆存储：容器 {@code ChatMemoryStore} Bean（mem0 / 自定义实现），供脚本自建助手复用 */
	private final ChatMemoryStore memoryStore;

	/** 无依赖构造：仅 {@code assistant()} 可用；其余方法会提示未配置 */
	public Langchain4jModule() {
		this(null, null, null, null, null, null);
	}

	public Langchain4jModule(Langchain4jSimpleAssistantProperties properties,
			ObjectProvider<Langchain4jChatModelModule> chatModelModules,
			ObjectProvider<Langchain4jLanguageModelModule> languageModelModules,
			ObjectProvider<Langchain4jStreamingLanguageModelModule> streamingLanguageModelModules,
			SimpleAssistant simpleAssistant,
			ObjectProvider<ChatMemoryStore> chatMemoryStores) {
		String key = properties == null ? null : properties.getKey();
		// 3 个单能力对象在构造方法里获取：MagicDynamic 代理包装（module + key，实时解析防实例失效），
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
		// 第 4 个：全能力助手由外面的 bean 注入（Langchain4jModuleConfiguration#simpleAssistant）
		this.simpleAssistant = simpleAssistant;
		// 容器 ChatMemoryStore（有 Bean 即暴露给脚本；缺席则 memoryStore() 方法侧提示）
		this.memoryStore = chatMemoryStores == null ? null : chatMemoryStores.getIfAvailable();
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

	/** 全能力助手（容器注入）：chat / stream / 补全 / 记忆。 */
	public SimpleAssistant simpleAssistant() {
		return require(simpleAssistant, "simple-assistant（请配置 fw.plus.magicapi.langchain4j.simple-assistant.enabled=true "
				+ "及 keys.<类型>）");
	}

	/**
	 * 会话记忆存储（容器 {@code ChatMemoryStore} Bean：mem0 / 自定义实现）：给脚本自建助手复用
	 * 容器同一份存储——{@code assistant().memoryStore(langchain4j.memoryStore())}。
	 *
	 * <p>不传 store 时 {@code AssistantBuilder} 每次 {@code build()} 都自带进程内 store，
	 * 会话历史随本次脚本调用结束而丢失，且 {@code chat-memory.type=mem0} 等容器配置对
	 * 自建助手不生效。</p>
	 */
	public ChatMemoryStore memoryStore() {
		if (memoryStore == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.Langchain4jModule.chatMemoryStoreMissing"));
		}
		return memoryStore;
	}

	private SimpleAssistant require(SimpleAssistant assistant, String resourceType) {
		if (assistant == null) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.Langchain4jModule.assistantMissing", resourceType));
		}
		return assistant;
	}
}
