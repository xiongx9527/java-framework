package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ocigenai;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiChatModel;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;

import java.util.List;

/**
 * Oracle OCI GenAI 工厂：认证经 OCI 配置文件（~/.oci/config），options 必填
 * {@code compartment-id} / {@code model-name}，可选 {@code config-path}（默认 ~/.oci/config）、
 * {@code config-profile}（默认 DEFAULT）、{@code region}。
 */
public class Langchain4jOciGenAiModelFactory implements ModelFactory {

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    @Override
    public String provider() {
        return "oci-genai";
    }

    @Override
    public OciGenAiChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        String regionCode = o.string("region", null);
        return OciGenAiChatModel.builder()
                .authProvider(authProvider(o))
                .region(regionCode == null ? null : Region.fromRegionCodeOrId(regionCode))
                .compartmentId(o.required("compartment-id"))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .topK(nullIfZero(o.integer("top-k", null)))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .listeners(listeners)
                .build();
    }

    @Override
    public OciGenAiStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        String regionCode = o.string("region", null);
        return OciGenAiStreamingChatModel.builder()
                .authProvider(authProvider(o))
                .region(regionCode == null ? null : Region.fromRegionCodeOrId(regionCode))
                .compartmentId(o.required("compartment-id"))
                .modelName(o.required("model-name"))
                .temperature(o.dbl("temperature", DEFAULT_TEMPERATURE))
                .topP(o.dbl("top-p", null))
                .maxTokens(nullIfZero(o.integer("max-tokens", null)))
                .listeners(listeners)
                .build();
    }

    private ConfigFileAuthenticationDetailsProvider authProvider(Langchain4jOptionsReader o) {
        try {
            String path = o.string("config-path", null);
            String profile = o.string("config-profile", "DEFAULT");
            return path == null
                    ? new ConfigFileAuthenticationDetailsProvider(profile)
                    : new ConfigFileAuthenticationDetailsProvider(path, profile);
        } catch (Exception e) {
            throw new IllegalStateException(I18nMessageResolver.resolveKey(
                    "fw.ai.Langchain4jOciGenAiModelFactory.authConfigLoadFailed", e.getMessage()), e);
        }
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
