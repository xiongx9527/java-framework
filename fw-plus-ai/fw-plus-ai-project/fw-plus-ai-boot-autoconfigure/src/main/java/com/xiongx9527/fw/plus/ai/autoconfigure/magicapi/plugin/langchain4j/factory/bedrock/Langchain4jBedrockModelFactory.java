package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.bedrock;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import software.amazon.awssdk.regions.Region;

import java.util.List;

/**
 * AWS Bedrock 工厂：凭据走 AWS 默认凭据链（环境变量/配置文件/角色），
 * options 只提供 {@code region}（必填）与 {@code model-id}（必填，如 anthropic.claude-3-5-sonnet-...）。
 * 注意：1.20.2 的 Bedrock builder 不暴露 temperature/topP/maxTokens，推理参数走模型默认。
 */
public class Langchain4jBedrockModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "bedrock";
    }

    @Override
    public BedrockChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return BedrockChatModel.builder()
                .region(Region.of(o.required("region")))
                .modelId(o.required("model-id"))
                .timeout(o.duration("timeout", null))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public BedrockStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        return BedrockStreamingChatModel.builder()
                .region(Region.of(o.required("region")))
                .modelId(o.required("model-id"))
                .timeout(o.duration("timeout", null))
                .listeners(listeners)
                .build();
    }
}
