package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.vertexai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.gemini.VertexAiGeminiStreamingChatModel;
import java.util.List;

/**
 * Vertex AI Gemini 工厂：dev.langchain4j.model.vertexai.* builder 链（需 GCP project/location）。
 * 仅 chat/streaming-chat 两类；temperature/topP 为 Float 型。
 */
public class Langchain4jVertexAiGeminiModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "vertex-ai-gemini";
    }

    @Override
    public VertexAiGeminiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return VertexAiGeminiChatModel.builder()
                .project(o.required("project"))
                .location(o.string("location", "us-central1"))
                .modelName(o.string("model-name", "gemini-1.5-flash"))
                .temperature(o.floatValue("temperature", null))
                .topP(o.floatValue("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxOutputTokens(nullIfZero(o.integer("max-output-tokens", null)))
                .seed(nullIfZero(o.integer("seed", null)))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .responseMimeType(o.string("response-mime-type", null))
                .useGoogleSearch(o.bool("use-google-search", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public VertexAiGeminiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return VertexAiGeminiStreamingChatModel.builder()
                .project(o.required("project"))
                .location(o.string("location", "us-central1"))
                .modelName(o.string("model-name", "gemini-1.5-flash"))
                .temperature(o.floatValue("temperature", null))
                .topP(o.floatValue("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxOutputTokens(nullIfZero(o.integer("max-output-tokens", null)))
                .responseMimeType(o.string("response-mime-type", null))
                .useGoogleSearch(o.bool("use-google-search", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
