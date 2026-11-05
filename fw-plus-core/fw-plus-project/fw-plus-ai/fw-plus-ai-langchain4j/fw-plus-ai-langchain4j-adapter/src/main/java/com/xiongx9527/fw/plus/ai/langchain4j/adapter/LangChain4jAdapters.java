package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.base.retrieval.VectorRetriever;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.model.LangChain4jEmbeddingPort;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.model.LangChain4jLlmPort;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.model.LangChain4jModerationModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.model.LangChain4jRerankPort;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.model.LangChain4jTokenCountEstimator;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.retrieval.LangChain4jContentRetrieverAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingRemover;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.LangChain4jStorePort;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.tool.LangChain4jToolProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 装配入口：把一组 LangChain4j 0.35 实例一次性适配成 base 端口。
 *
 * <p>只做便利装配，不创建、不绑定具体 provider，也不做 Spring 自动配置；
 * 使用哪个厂商的模型 / 存储由业务侧决定并注入。</p>
 */
public final class LangChain4jAdapters {

    private LangChain4jAdapters() {
    }

    public static LangChain4jLlmPort llm(ChatLanguageModel chatModel) {
        return new LangChain4jLlmPort(chatModel);
    }

    public static LangChain4jLlmPort llm(ChatLanguageModel chatModel,
                                        StreamingChatLanguageModel streamingModel) {
        return new LangChain4jLlmPort(chatModel, streamingModel);
    }

    public static LangChain4jEmbeddingPort embedding(EmbeddingModel model) {
        return new LangChain4jEmbeddingPort(model);
    }

    public static LangChain4jRerankPort rerank(ScoringModel model) {
        return new LangChain4jRerankPort(model);
    }

    public static LangChain4jModerationModel moderation(
            dev.langchain4j.model.moderation.ModerationModel model) {
        return new LangChain4jModerationModel(model);
    }

    public static LangChain4jTokenCountEstimator tokenCounter(Tokenizer tokenizer) {
        return new LangChain4jTokenCountEstimator(tokenizer);
    }

    /** 0.35 只有一种存储抽象，因此只有一个 Store 适配器。 */
    public static LangChain4jStorePort store(EmbeddingStore<TextSegment> store) {
        return new LangChain4jStorePort(store);
    }

    /** 确认后端支持 filter / 需要自定义删除时，用这个重载显式配置。 */
    public static LangChain4jStorePort store(EmbeddingStore<TextSegment> store,
                                             boolean pushdownFilters, EmbeddingRemover remover) {
        return new LangChain4jStorePort(store, "langchain4j", "default", pushdownFilters, remover);
    }

    /** LangChain4j 的 ToolProvider → base 的 ToolProvider。 */
    public static LangChain4jToolProviderAdapter toolProvider(
            dev.langchain4j.service.tool.ToolProvider provider) {
        return new LangChain4jToolProviderAdapter(provider);
    }

    /** base 的 ToolProvider → LangChain4j 的 ToolProvider（可挂到 AiServices）。 */
    public static dev.langchain4j.service.tool.ToolProvider toolProvider(ToolProvider provider) {
        return LangChain4jToolProviderAdapter.toUpstream(provider);
    }

    public static LangChain4jContentRetrieverAdapter contentRetriever(ContentRetriever retriever) {
        return new LangChain4jContentRetrieverAdapter(retriever);
    }

    /** 复用 base 的 {@link VectorRetriever}：向量化 + 最近邻检索一条链。 */
    public static VectorRetriever vectorRetriever(EmbeddingModel embedding,
                                                  EmbeddingStore<TextSegment> store) {
        return new VectorRetriever(embedding(embedding), store(store));
    }
}
