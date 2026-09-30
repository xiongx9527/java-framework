package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.util.List;

/**
 * OpenAI 工厂：dev.langchain4j.model.openai.* builder 链。
 * 注意 language/streaming-language 两个 builder 的能力集比 chat 少（无 topP/stop/maxTokens 等），
 * 语言类模型只映射其真实支持的 setter。
 */
public class Langchain4jOpenAiModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public OpenAiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return OpenAiChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .organizationId(o.string("organization-id", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .stop(o.stringList("stop", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public OpenAiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        // 注意：0.35.0 streaming builder 无 maxRetries（流式请求不重试）
        return OpenAiStreamingChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .organizationId(o.string("organization-id", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .stop(o.stringList("stop", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }



    @Override
    public OpenAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .organizationId(o.string("organization-id", null))
                .modelName(o.required("model-name"))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    @Override
    public TokenCountEstimator buildTokenizer(Langchain4jOptionsReader o) {
        return new OpenAiTokenCountEstimator(o.required("api-key"));
    }

    /** reader 的 integer(name,null) 键缺失返回 0；对 Integer 形参归零为 null 表示未配置 */
    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
