package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.workersai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.workersai.WorkersAiChatModel;
import dev.langchain4j.model.workersai.WorkersAiEmbeddingModel;

import java.util.List;

/**
 * Cloudflare Workers AI 工厂：options 必填 {@code account-id} / {@code api-token} /
 * {@code model-name}（如 @cf/meta/llama-3.1-8b-instruct）。builder 只暴露这三项。
 */
public class Langchain4jWorkersAiModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "workers-ai";
    }

    @Override
    public WorkersAiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return WorkersAiChatModel.builder()
                .accountId(o.required("account-id"))
                .apiToken(o.required("api-token"))
                .modelName(o.required("model-name"))
                .build();
    }

    @Override
    public WorkersAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return WorkersAiEmbeddingModel.builder()
                .accountId(o.required("account-id"))
                .apiToken(o.required("api-token"))
                .modelName(o.required("model-name"))
                .build();
    }
}
