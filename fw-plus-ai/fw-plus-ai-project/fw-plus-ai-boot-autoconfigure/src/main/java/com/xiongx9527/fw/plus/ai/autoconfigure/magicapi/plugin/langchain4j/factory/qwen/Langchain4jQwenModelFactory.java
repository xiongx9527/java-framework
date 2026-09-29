package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.qwen;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.dashscope.QwenLanguageModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingLanguageModel;
import dev.langchain4j.model.dashscope.QwenTokenizer;
import java.util.Collections;
import java.util.List;

/**
 * 通义千问（dashscope）工厂：dev.langchain4j.model.dashscope.Qwen* builder 链。
 * qwen 四个生成类 builder 签名一致：baseUrl/apiKey/modelName/temperature(Float)/topP/topK/
 * enableSearch/seed/repetitionPenalty(Float)/stops/maxTokens。
 */
public class Langchain4jQwenModelFactory implements ModelFactory {

    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final Float DEFAULT_TEMPERATURE = 0.7f;
    private static final String DEFAULT_MODEL = "qwen-plus";
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-v3";

    @Override
    public String provider() {
        return "qwen";
    }

    @Override
    public QwenChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return QwenChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.floatValue("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .enableSearch(o.bool("enable-search", false))
                .seed(nullIfZero(o.integer("seed", null)))
                .repetitionPenalty(o.floatValue("repetition-penalty", null))
                .stops(o.stringList("stops", Collections.<String>emptyList()))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .listeners(listeners)
                .build();
    }

    @Override
    public QwenStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return QwenStreamingChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.floatValue("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .enableSearch(o.bool("enable-search", false))
                .seed(nullIfZero(o.integer("seed", null)))
                .repetitionPenalty(o.floatValue("repetition-penalty", null))
                .stops(o.stringList("stops", Collections.<String>emptyList()))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .listeners(listeners)
                .build();
    }

    @Override
    public QwenLanguageModel buildLanguageModel(Langchain4jOptionsReader o) {
        return QwenLanguageModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.floatValue("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .enableSearch(o.bool("enable-search", false))
                .seed(nullIfZero(o.integer("seed", null)))
                .repetitionPenalty(o.floatValue("repetition-penalty", null))
                .stops(o.stringList("stops", Collections.<String>emptyList()))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .build();
    }

    @Override
    public QwenStreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) {
        return QwenStreamingLanguageModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.floatValue("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .enableSearch(o.bool("enable-search", false))
                .seed(nullIfZero(o.integer("seed", null)))
                .repetitionPenalty(o.floatValue("repetition-penalty", null))
                .stops(o.stringList("stops", Collections.<String>emptyList()))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .build();
    }

    @Override
    public QwenEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return QwenEmbeddingModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_EMBEDDING_MODEL))
                .build();
    }

    @Override
    public Tokenizer buildTokenizer(Langchain4jOptionsReader o) {
        return QwenTokenizer.builder()
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
