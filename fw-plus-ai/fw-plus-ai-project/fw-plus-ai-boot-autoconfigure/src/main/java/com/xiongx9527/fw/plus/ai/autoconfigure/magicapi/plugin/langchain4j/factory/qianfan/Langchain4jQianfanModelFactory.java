package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.qianfan;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.qianfan.QianfanChatModel;
import dev.langchain4j.model.qianfan.QianfanEmbeddingModel;
import dev.langchain4j.model.qianfan.QianfanLanguageModel;
import dev.langchain4j.model.qianfan.QianfanStreamingChatModel;
import dev.langchain4j.model.qianfan.QianfanStreamingLanguageModel;
import java.util.List;

/**
 * 百度千帆（baidu qianfan）工厂：dev.langchain4j.model.qianfan.* builder 链。
 * 千帆需要 apiKey + secretKey 双凭证；key 为 kebab-case：api-key/secret-key。
 */
public class Langchain4jQianfanModelFactory implements ModelFactory {

    // 注意：0.35.0 该 SDK builder 无 listeners(...)，本 provider 忽略 listeners

    @Override
    public String provider() {
        return "qianfan";
    }

    @Override
    public QianfanChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return QianfanChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .secretKey(o.required("secret-key"))
                .modelName(o.string("model-name", "ERNIE-Bot-turbo"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .penaltyScore(o.dbl("penalty-score", null))
                .maxOutputTokens(nullIfZero(o.integer("max-output-tokens", null)))
                .stop(o.stringList("stop", null))
                .endpoint(o.string("endpoint", null))
                .responseFormat(o.string("response-format", null))
                .userId(o.string("user-id", null))
                .system(o.string("system", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public QianfanStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return QianfanStreamingChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .secretKey(o.required("secret-key"))
                .modelName(o.string("model-name", "ERNIE-Bot-turbo"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .penaltyScore(o.dbl("penalty-score", null))
                .endpoint(o.string("endpoint", null))
                .responseFormat(o.string("response-format", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public QianfanLanguageModel buildLanguageModel(Langchain4jOptionsReader o) {
        return QianfanLanguageModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .secretKey(o.required("secret-key"))
                .modelName(o.string("model-name", "ERNIE-Bot-turbo"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .penaltyScore(o.dbl("penalty-score", null))
                .endpoint(o.string("endpoint", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public QianfanStreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) {
        return QianfanStreamingLanguageModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .secretKey(o.required("secret-key"))
                .modelName(o.string("model-name", "ERNIE-Bot-turbo"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .penaltyScore(o.dbl("penalty-score", null))
                .endpoint(o.string("endpoint", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public QianfanEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return QianfanEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .secretKey(o.required("secret-key"))
                .modelName(o.string("model-name", "bge-large-zh"))
                .user(o.string("user", null))
                .endpoint(o.string("endpoint", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
