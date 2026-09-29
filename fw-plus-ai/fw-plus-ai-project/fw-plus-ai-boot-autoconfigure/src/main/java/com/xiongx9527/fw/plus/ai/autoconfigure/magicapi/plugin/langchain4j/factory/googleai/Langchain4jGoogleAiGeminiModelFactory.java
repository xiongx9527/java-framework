package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.googleai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.util.List;

/**
 * Google AI Gemini 工厂：dev.langchain4j.model.googleai.* builder 链。
 * 仅有 chat 与 embedding 两类能力（0.35.0 无 streaming/language）。
 * 注意日志参数为单个 logRequestsAndResponses。
 */
public class Langchain4jGoogleAiGeminiModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "google-ai-gemini";
    }

    @Override
    public GoogleAiGeminiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", "gemini-2.0-flash"))
                .temperature(o.dbl("temperature", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .topP(o.dbl("top-p", null))
                .maxOutputTokens(nullIfZero(o.integer("max-output-tokens", null)))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .timeout(o.duration("timeout", null))
                .stopSequences(o.stringList("stop-sequences", null))
                .allowCodeExecution(o.bool("allow-code-execution", null))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public GoogleAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(o.required("api-key"))
                .modelName(o.string("model-name", "text-embedding-004"))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .timeout(o.duration("timeout", null))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
