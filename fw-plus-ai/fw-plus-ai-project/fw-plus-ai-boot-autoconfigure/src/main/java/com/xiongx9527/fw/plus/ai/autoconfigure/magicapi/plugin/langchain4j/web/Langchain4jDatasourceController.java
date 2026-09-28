package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jTokenizerInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * datasource 连接测试：按 body（datasource JSON）构建实例并做最小探测，不落库、不注册）。
 * 协议对齐官方 mongo datasource test：返回 "ok" 或中文错误信息。
 */
public class Langchain4jDatasourceController extends MagicController implements MagicExceptionHandler {

    public Langchain4jDatasourceController(MagicConfiguration configuration) {
        super(configuration);
    }

    private Langchain4jModelFactoryRegistry factories() {
        return SpringUtils.getBean(Langchain4jModelFactoryRegistry.class);
    }

    private MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels() {
        return SpringUtils.getBean(MagicDynamicLangchain4jEmbeddingModelDataSource.class);
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
                embeddingModel = embeddingModels().get(
                        ((Langchain4jEmbeddingStoreInfo) info).getEmbeddingModelKey());
            }
            Object instance = factories().build(modelType, info, embeddingModel);
            probe(modelType, instance, embeddingModel);
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
            case TOKENIZER:
                ((Tokenizer) instance).estimateTokenCountInText("test");
                break;
            case EMBEDDING_STORE:
                if (embeddingModel == null) {
                    throw new IllegalArgumentException("embedding-model 未配置或未启用，无法探测 embedding-store");
                }
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

    @SuppressWarnings("unchecked")
    private EmbeddingStore<TextSegment> castStore(Object instance) {
        return (EmbeddingStore<TextSegment>) instance;
    }
}
