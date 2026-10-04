package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.watsonx;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.watsonx.WatsonxChatModel;
import dev.langchain4j.model.watsonx.WatsonxEmbeddingModel;
import dev.langchain4j.model.watsonx.WatsonxStreamingChatModel;

import java.util.List;

/**
 * IBM watsonx.ai 工厂：options 必填 {@code base-url} / {@code api-key} / {@code project-id} /
 * {@code model-name}（如 ibm/granite-3-8b-instruct）。
 */
public class Langchain4jWatsonxModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "watsonx";
    }

    @Override
    public WatsonxChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return WatsonxChatModel.builder()
                .baseUrl(o.required("base-url"))
                .apiKey(o.required("api-key"))
                .projectId(o.required("project-id"))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxOutputTokens(nullIfZero(o.integer("max-output-tokens", null)))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .presencePenalty(o.dbl("presence-penalty", null))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public WatsonxStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        WatsonxStreamingChatModel.Builder builder = WatsonxStreamingChatModel.builder()
                .baseUrl(o.required("base-url"))
                .apiKey(o.required("api-key"))
                .projectId(o.required("project-id"))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxOutputTokens(nullIfZero(o.integer("max-output-tokens", null)))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners);
        if (o.has("thinking")) {
            builder.thinking(o.bool("thinking", null));
        }
        return builder.build();
    }

    @Override
    public WatsonxEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return WatsonxEmbeddingModel.builder()
                .baseUrl(o.required("base-url"))
                .apiKey(o.required("api-key"))
                .projectId(o.required("project-id"))
                .modelName(o.required("model-name"))
                .timeout(o.duration("timeout", null))
                .listeners(null)
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
