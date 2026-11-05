package com.xiongx9527.fw.plus.ai.langchain4j.adapter.support;

import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolParameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 工具规格与工具调用请求在 base 与 LangChain4j 0.35 之间的转换。 */
public final class LcTools {

    private LcTools() {
    }

    public static List<dev.langchain4j.agent.tool.ToolSpecification> toUpstreamSpecs(
            List<ToolSpecification> specs) {
        List<dev.langchain4j.agent.tool.ToolSpecification> out =
                new ArrayList<dev.langchain4j.agent.tool.ToolSpecification>();
        if (specs == null) {
            return out;
        }
        for (ToolSpecification spec : specs) {
            if (spec != null) {
                out.add(toUpstreamSpec(spec));
            }
        }
        return out;
    }

    public static dev.langchain4j.agent.tool.ToolSpecification toUpstreamSpec(ToolSpecification spec) {
        Map<String, Object> raw = spec.getParameters();
        Map<String, Map<String, Object>> properties = new LinkedHashMap<String, Map<String, Object>>();
        List<String> required = new ArrayList<String>();
        String type = "object";
        if (raw != null) {
            if (raw.get("type") != null) {
                type = String.valueOf(raw.get("type"));
            }
            Object props = raw.get("properties");
            if (props instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) props).entrySet()) {
                    Object schema = entry.getValue();
                    if (schema instanceof Map) {
                        Map<String, Object> normalized = new LinkedHashMap<String, Object>();
                        for (Map.Entry<?, ?> property : ((Map<?, ?>) schema).entrySet()) {
                            normalized.put(String.valueOf(property.getKey()), property.getValue());
                        }
                        properties.put(String.valueOf(entry.getKey()), normalized);
                    }
                }
            }
            Object req = raw.get("required");
            if (req instanceof Collection) {
                for (Object value : (Collection<?>) req) {
                    if (value != null) {
                        required.add(String.valueOf(value));
                    }
                }
            }
        }
        ToolParameters parameters = ToolParameters.builder()
                .type(type)
                .properties(properties)
                .required(required)
                .build();
        return dev.langchain4j.agent.tool.ToolSpecification.builder()
                .name(spec.getName())
                .description(spec.getDescription() == null ? "" : spec.getDescription())
                .parameters(parameters)
                .build();
    }

    public static List<dev.langchain4j.agent.tool.ToolExecutionRequest> toUpstreamCalls(
            List<ToolExecutionRequest> calls) {
        List<dev.langchain4j.agent.tool.ToolExecutionRequest> out =
                new ArrayList<dev.langchain4j.agent.tool.ToolExecutionRequest>();
        if (calls == null) {
            return out;
        }
        for (ToolExecutionRequest call : calls) {
            if (call == null) {
                continue;
            }
            out.add(dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                    .id(call.getId())
                    .name(call.getName())
                    .arguments(call.getArguments())
                    .build());
        }
        return out;
    }

    public static dev.langchain4j.agent.tool.ToolExecutionRequest toUpstreamCall(
            ToolExecutionRequest call) {
        if (call == null) {
            return null;
        }
        return dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                .id(call.getId())
                .name(call.getName())
                .arguments(call.getArguments())
                .build();
    }

    public static ToolExecutionRequest toBaseCall(dev.langchain4j.agent.tool.ToolExecutionRequest call) {
        if (call == null) {
            return null;
        }
        return new ToolExecutionRequest(call.id(), call.name(), call.arguments());
    }
}
