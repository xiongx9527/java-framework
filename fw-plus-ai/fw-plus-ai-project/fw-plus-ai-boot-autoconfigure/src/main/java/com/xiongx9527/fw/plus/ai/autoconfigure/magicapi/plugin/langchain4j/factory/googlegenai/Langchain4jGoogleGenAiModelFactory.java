package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.googlegenai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import dev.langchain4j.model.google.genai.GoogleGenAiEmbeddingModel;
import dev.langchain4j.model.google.genai.GoogleGenAiStreamingChatModel;

import java.util.List;

/**
 * Google GenAI 工厂（Gemini 新版 API）：apiKey 模式（AI Studio）直连；
 * Vertex AI 模式不传 api-key，改传 project-id + location。
 */
public class Langchain4jGoogleGenAiModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "google-genai";
    }

    @Override
    public GoogleGenAiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return GoogleGenAiChatModel.builder()
                .apiKey(o.string("api-key", null))
                .projectId(o.string("project-id", null))
                .location(o.string("location", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxOutputTokens(nullIfZero(o.integer("max-output-tokens", null)))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public GoogleGenAiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        return GoogleGenAiStreamingChatModel.builder()
                .apiKey(o.string("api-key", null))
                .projectId(o.string("project-id", null))
                .location(o.string("location", null))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxOutputTokens(nullIfZero(o.integer("max-output-tokens", null)))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public GoogleGenAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return GoogleGenAiEmbeddingModel.builder()
                .apiKey(o.string("api-key", null))
                .projectId(o.string("project-id", null))
                .location(o.string("location", null))
                .modelName(o.required("model-name"))
                .outputDimensionality(nullIfZero(o.integer("output-dimensionality", null)))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
