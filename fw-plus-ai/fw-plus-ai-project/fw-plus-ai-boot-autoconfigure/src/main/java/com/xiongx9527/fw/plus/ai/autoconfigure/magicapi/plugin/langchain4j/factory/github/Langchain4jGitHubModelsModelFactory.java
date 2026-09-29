package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.github;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.github.GitHubModelsChatModel;
import dev.langchain4j.model.github.GitHubModelsEmbeddingModel;
import dev.langchain4j.model.github.GitHubModelsStreamingChatModel;
import java.util.List;

/**
 * GitHub Models 工厂：dev.langchain4j.model.github.* builder 链。
 * 凭证为 gitHubToken（GitHub PAT）；seed 为 Long 型；日志参数为单个 logRequestsAndResponses。
 */
public class Langchain4jGitHubModelsModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "github-models";
    }

    @Override
    public GitHubModelsChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return GitHubModelsChatModel.builder()
                .gitHubToken(o.required("github-token"))
                .endpoint(o.string("endpoint", null))
                .modelName(o.string("model-name", "gpt-4o"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .stop(o.stringList("stop", null))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .seed(o.longValue("seed", null) == 0 ? null : o.longValue("seed", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public GitHubModelsStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return GitHubModelsStreamingChatModel.builder()
                .gitHubToken(o.required("github-token"))
                .endpoint(o.string("endpoint", null))
                .modelName(o.string("model-name", "gpt-4o"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .stop(o.stringList("stop", null))
                .presencePenalty(o.dbl("presence-penalty", null))
                .frequencyPenalty(o.dbl("frequency-penalty", null))
                .timeout(o.duration("timeout", null))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public GitHubModelsEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return GitHubModelsEmbeddingModel.builder()
                .gitHubToken(o.required("github-token"))
                .endpoint(o.string("endpoint", null))
                .modelName(o.string("model-name", "text-embedding-3-small"))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .logRequestsAndResponses(o.bool("log-requests-and-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
