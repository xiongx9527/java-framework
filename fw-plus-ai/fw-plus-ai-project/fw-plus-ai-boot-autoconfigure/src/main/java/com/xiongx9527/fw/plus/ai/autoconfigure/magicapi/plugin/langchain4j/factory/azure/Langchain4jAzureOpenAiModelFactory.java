package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.azure;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.azure.AzureOpenAiLanguageModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingLanguageModel;
import dev.langchain4j.model.azure.AzureOpenAiTokenCountEstimator;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import java.util.List;

/**
 * Azure OpenAI 工厂：dev.langchain4j.model.azure.* builder 链。
 * 关键差异：endpoint + apiKey + deploymentName（部署名，非模型名）。
 * AzureOpenAiTokenCountEstimator 按默认模型名估算，不要求凭证。
 */
public class Langchain4jAzureOpenAiModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "azure-openai";
    }

    @Override
    public AzureOpenAiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return AzureOpenAiChatModel.builder()
                .endpoint(o.required("endpoint"))
                .apiKey(o.required("api-key"))
                .deploymentName(o.required("deployment-name"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .stop(o.stringList("stop", null))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .user(o.string("user", null))
                .seed(o.longValue("seed", null) == 0 ? null : o.longValue("seed", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public AzureOpenAiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return AzureOpenAiStreamingChatModel.builder()
                .endpoint(o.required("endpoint"))
                .apiKey(o.required("api-key"))
                .deploymentName(o.required("deployment-name"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .stop(o.stringList("stop", null))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .user(o.string("user", null))
                .timeout(o.duration("timeout", null))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .listeners(listeners)
                .build();
    }



    @Override
    public AzureOpenAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return AzureOpenAiEmbeddingModel.builder()
                .endpoint(o.required("endpoint"))
                .apiKey(o.required("api-key"))
                .deploymentName(o.required("deployment-name"))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .build();
    }

    @Override
    public AzureOpenAiLanguageModel buildLanguageModel(Langchain4jOptionsReader o) {
        return AzureOpenAiLanguageModel.builder()
                .endpoint(o.required("endpoint"))
                .apiKey(o.required("api-key"))
                .deploymentName(o.required("deployment-name"))
                .temperature(o.dbl("temperature", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .build();
    }

    @Override
    public AzureOpenAiStreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) {
        return AzureOpenAiStreamingLanguageModel.builder()
                .endpoint(o.required("endpoint"))
                .apiKey(o.required("api-key"))
                .deploymentName(o.required("deployment-name"))
                .temperature(o.dbl("temperature", null))
                .timeout(o.duration("timeout", null))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .build();
    }

    @Override
    public TokenCountEstimator buildTokenizer(Langchain4jOptionsReader o) {
        // 1.x 无无参构造；按默认模型名估算，token 统计为估算值
        return new AzureOpenAiTokenCountEstimator("gpt-4o");
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
