package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.zhipu;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.community.model.zhipu.chat.Thinking;
import java.util.List;

/**
 * 智谱（zhipu-ai）工厂：dev.langchain4j.community.model.zhipu.* builder 链。
 * 注意参数名与别家不同：model（而非 modelName）、maxToken、callTimeout/connectTimeout/readTimeout/writeTimeout。
 */
public class Langchain4jZhipuModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "zhipu-ai";
    }

    @Override
    public ZhipuAiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return ZhipuAiChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .model(o.string("model", "glm-4"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .stops(o.stringList("stops", null))
                .maxToken(nullIfZero(o.integer("max-token", null)))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .callTimeout(o.duration("call-timeout", null))
                .connectTimeout(o.duration("connect-timeout", null))
                .readTimeout(o.duration("read-timeout", null))
                .writeTimeout(o.duration("write-timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public ZhipuAiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        ZhipuAiStreamingChatModel.ZhipuAiStreamingChatModelBuilder builder = ZhipuAiStreamingChatModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .model(o.string("model", "glm-4"))
                .temperature(o.dbl("temperature", null))
                .topP(o.dbl("top-p", null))
                .stops(o.stringList("stops", null))
                .maxToken(nullIfZero(o.integer("max-token", null)))
                .callTimeout(o.duration("call-timeout", null))
                .connectTimeout(o.duration("connect-timeout", null))
                .readTimeout(o.duration("read-timeout", null))
                .writeTimeout(o.duration("write-timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners);
        if (o.has("thinking-type") || o.has("clear-thinking")) {
            Thinking.Builder thinking = Thinking.builder();
            if (o.has("thinking-type")) {
                thinking.type(o.string("thinking-type", null));
            }
            if (o.has("clear-thinking")) {
                thinking.clearThinking(o.bool("clear-thinking", null));
            }
            builder.thinking(thinking.build());
        }
        return builder.build();
    }

    @Override
    public ZhipuAiEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return ZhipuAiEmbeddingModel.builder()
                .baseUrl(o.string("base-url", null))
                .apiKey(o.required("api-key"))
                .model(o.string("model", "embedding-2"))
                .dimensions(nullIfZero(o.integer("dimensions", null)))
                .maxRetries(nullIfZero(o.integer("max-retries", null)))
                .callTimeout(o.duration("call-timeout", null))
                .connectTimeout(o.duration("connect-timeout", null))
                .readTimeout(o.duration("read-timeout", null))
                .writeTimeout(o.duration("write-timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
