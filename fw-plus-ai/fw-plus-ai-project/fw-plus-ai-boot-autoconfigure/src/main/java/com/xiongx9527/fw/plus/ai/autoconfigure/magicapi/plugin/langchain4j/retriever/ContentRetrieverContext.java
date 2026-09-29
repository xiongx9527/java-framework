package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chat.MagicDynamicChatLanguageModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.embedding.MagicDynamicEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.MagicDynamicLangchain4jContentRetrieverDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.web.search.WebSearchEngine;
import org.springframework.beans.factory.ObjectProvider;

/**
 * provider 构建 ContentRetriever 时可取用的依赖视图：按 datasource key 解析被引用的实例。
 *
 * <p>模型类依赖走懒解析（{@code MagicDynamic*} 包装）：注册顺序不影响构建，真正取用发生在检索时；
 * 向量库直接按 key 取（检索要用其 search 能力，懒包装无法透传）。</p>
 */
public class ContentRetrieverContext {

	private final ObjectProvider<Langchain4jEmbeddingStoreModule> embeddingStores;

	private final ObjectProvider<Langchain4jEmbeddingModelModule> embeddingModels;

	private final ObjectProvider<Langchain4jChatModelModule> chatModels;

	private final MagicDynamicLangchain4jContentRetrieverDataSource retrievers;

	private final ObjectProvider<WebSearchEngine> webSearchEngines;

	private final MagicToolInvoker invoker;

	public ContentRetrieverContext(ObjectProvider<Langchain4jEmbeddingStoreModule> embeddingStores,
			ObjectProvider<Langchain4jEmbeddingModelModule> embeddingModels,
			ObjectProvider<Langchain4jChatModelModule> chatModels,
			MagicDynamicLangchain4jContentRetrieverDataSource retrievers,
			ObjectProvider<WebSearchEngine> webSearchEngines,
			MagicToolInvoker invoker) {
		this.embeddingStores = embeddingStores;
		this.embeddingModels = embeddingModels;
		this.chatModels = chatModels;
		this.retrievers = retrievers;
		this.webSearchEngines = webSearchEngines;
		this.invoker = invoker;
	}

	/** 按 key 取 embedding-store；key 为空取 Spring 默认实例 */
	public EmbeddingStore<TextSegment> embeddingStore(String key) {
		return require(embeddingStores, "langchain4j-embedding-store").getDynamicAttribute(key);
	}

	/** 按 key 懒取 embedding-model；key 为空取 Spring 默认实例 */
	public EmbeddingModel embeddingModel(String key) {
		return new MagicDynamicEmbeddingModel(require(embeddingModels, "langchain4j-embedding-model"), key);
	}

	/** 按 key 懒取 chat-model（neo4j 用它把自然语言转成 Cypher）；key 为空取 Spring 默认实例 */
	public ChatLanguageModel chatModel(String key) {
		return new MagicDynamicChatLanguageModel(require(chatModels, "langchain4j-chat-model"), key);
	}

	/** 按 key 懒取另一个 ContentRetriever（filtering 的委托），支持链式引用与乱序注册 */
	public ContentRetriever contentRetriever(String key) {
		return new MagicDynamicContentRetriever(retrievers, key);
	}

	/**
	 * 搜索引擎：配置了 {@code searchFunction} 时用 magic-api 函数托管（无需第三方 SDK），
	 * 否则取 Spring 容器中的 {@link WebSearchEngine} 实例。
	 */
	public WebSearchEngine webSearchEngine(String searchFunction) {
		if (searchFunction != null && !searchFunction.trim().isEmpty()) {
			return new MagicApiWebSearchEngine(invoker, searchFunction);
		}
		WebSearchEngine engine = webSearchEngines == null ? null : webSearchEngines.getIfAvailable();
		if (engine == null) {
			throw new IllegalArgumentException("请在 options 中配置 search-function，"
					+ "或在 Spring 容器中注册一个 dev.langchain4j.web.search.WebSearchEngine 实例");
		}
		return engine;
	}

	private static <T> T require(ObjectProvider<T> provider, String type) {
		T module = provider == null ? null : provider.getIfAvailable();
		if (module == null) {
			throw new IllegalArgumentException(
					"当前应用未装配 " + type + " 数据源，无法构建依赖它的 langchain4j-content-retriever");
		}
		return module;
	}
}
