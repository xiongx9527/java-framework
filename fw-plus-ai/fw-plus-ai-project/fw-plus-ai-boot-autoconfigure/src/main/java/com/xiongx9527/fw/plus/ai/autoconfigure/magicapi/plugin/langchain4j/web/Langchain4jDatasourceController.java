package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jScoringModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jTokenizerInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jEmbeddingStoreMagicDynamicRegistry;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.Closeable;
import java.util.Map;

/**
 * datasource 连接测试：按 body（datasource JSON）构建实例并做最小探测，不落库、不注册。
 * 协议对齐官方 datasource test：返回 "ok" 或中文错误信息。
 */
public class Langchain4jDatasourceController extends MagicController implements MagicExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(Langchain4jDatasourceController.class);

    private final Langchain4jModelFactoryRegistry factories;
    /** 允许为 null：embedding-model 按类型独立装配，可能未启用（此时无法测试 embedding-store） */
    private final MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels;

    public Langchain4jDatasourceController(MagicConfiguration configuration,
            Langchain4jModelFactoryRegistry factories,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels) {
        super(configuration);
        this.factories = factories;
        this.embeddingModels = embeddingModels;
    }

    /**
     * @param type 资源类型：chat-model / streaming-chat-model / language-model / streaming-language-model /
     *             embedding-model / tokenizer / embedding-store
     * @param body datasource JSON（key/provider/options/embeddingModelKey）
     */
    @PostMapping("/langchain4j/{type}/datasource/test")
    @ResponseBody
    public String test(@PathVariable("type") String type, @RequestBody Map<String, Object> body) {
        try {
            Langchain4jModelTypes modelType = Langchain4jModelTypes.of("langchain4j-" + type);
            Langchain4jModelInfo info = toInfo(modelType, body);
            EmbeddingModel embeddingModel = null;
            if (modelType == Langchain4jModelTypes.EMBEDDING_STORE) {
                if (embeddingModels == null) {
                    return "当前应用未装配 LangChain4j embedding-model，无法测试 embedding-store";
                }
                embeddingModel = Langchain4jEmbeddingStoreMagicDynamicRegistry.requireEmbeddingModel(
                        (Langchain4jEmbeddingStoreInfo) info,
                        embeddingModels.get(((Langchain4jEmbeddingStoreInfo) info).getEmbeddingModelKey()));
            }
            Object instance = factories.build(modelType, info, embeddingModel);
            try {
                probe(modelType, instance, embeddingModel);
            } finally {
                closeProbeInstance(modelType, instance);
            }
            return "ok";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "测试失败: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Langchain4jModelInfo> T toInfo(Langchain4jModelTypes type, Map<String, Object> body) {
        byte[] json = JSON.toJSONBytes(body);
        switch (type) {
            case CHAT_MODEL:
                return (T) JSON.parseObject(json, Langchain4jChatModelInfo.class);
            case STREAMING_CHAT_MODEL:
                return (T) JSON.parseObject(json, Langchain4jStreamingChatModelInfo.class);
            case LANGUAGE_MODEL:
                return (T) JSON.parseObject(json, Langchain4jLanguageModelInfo.class);
            case STREAMING_LANGUAGE_MODEL:
                return (T) JSON.parseObject(json, Langchain4jStreamingLanguageModelInfo.class);
            case EMBEDDING_MODEL:
                return (T) JSON.parseObject(json, Langchain4jEmbeddingModelInfo.class);
            case SCORING_MODEL:
                return (T) JSON.parseObject(json, Langchain4jScoringModelInfo.class);
            case TOKENIZER:
                return (T) JSON.parseObject(json, Langchain4jTokenizerInfo.class);
            case EMBEDDING_STORE:
                return (T) JSON.parseObject(json, Langchain4jEmbeddingStoreInfo.class);
            default:
                throw new IllegalArgumentException("不支持的资源类型: " + type);
        }
    }

    private void probe(Langchain4jModelTypes type, Object instance, EmbeddingModel embeddingModel) {
        switch (type) {
            case CHAT_MODEL:
                ((ChatLanguageModel) instance).generate(UserMessage.from("ping"));
                break;
            case LANGUAGE_MODEL:
                ((LanguageModel) instance).generate("ping");
                break;
            case EMBEDDING_MODEL:
                ((EmbeddingModel) instance).embed("test");
                break;
            case SCORING_MODEL:
                ((ScoringModel) instance).scoreAll(java.util.Collections.singletonList(TextSegment.from("test")), "test");
                break;
            case EMBEDDING_STORE:
                EmbeddingStore<TextSegment> store = castStore(instance);
                Embedding embedding = embeddingModel.embed("test").content();
                store.add(embedding, TextSegment.from("probe"));
                store.search(EmbeddingSearchRequest.builder().queryEmbedding(embedding).maxResults(1).build());
                break;
            case STREAMING_CHAT_MODEL:
            case STREAMING_LANGUAGE_MODEL:
            default:
                // streaming 两类仅验证构建，不发起调用
                break;
        }
    }

    /** embedding-store 的探测实例是一次性的，探测结束后释放其连接。 */
    private void closeProbeInstance(Langchain4jModelTypes type, Object instance) {
        if (type != Langchain4jModelTypes.EMBEDDING_STORE || !(instance instanceof Closeable)) {
            return;
        }
        try {
            ((Closeable) instance).close();
        } catch (Exception e) {
            logger.warn("关闭连接测试实例失败: type={}", type.type(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private EmbeddingStore<TextSegment> castStore(Object instance) {
        return (EmbeddingStore<TextSegment>) instance;
    }
}
