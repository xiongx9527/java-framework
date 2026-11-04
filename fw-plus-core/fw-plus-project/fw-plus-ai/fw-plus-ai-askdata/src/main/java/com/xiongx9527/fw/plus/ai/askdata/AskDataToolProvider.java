package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.base.tool.ToolArgumentCodec;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * askdata.ask_data tool: exposes the end-to-end AskDataCapability to an agentic loop as a single tool.
 */
public class AskDataToolProvider implements ToolProvider {

    public static final String TOOL_NAME = "askdata.ask_data";

    private final AskDataCapability capability;
    private final ToolArgumentCodec codec;

    public AskDataToolProvider(AskDataCapability capability, ToolArgumentCodec codec) {
        if (capability == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.ask_data_capability_required"));
        }
        if (codec == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.codec_required"));
        }
        this.capability = capability;
        this.codec = codec;
    }

    @Override
    public List<ToolSpecification> specifications() {
        return Collections.singletonList(spec());
    }

    public static ToolSpecification spec() {
        Map<String, Object> question = stringProp("user natural-language question about the data");
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("question", question);
        List<String> required = Collections.singletonList("question");
        return ToolSpecification.builder()
                .name(TOOL_NAME)
                .description("Answer a data question end-to-end: recall schema, generate SQL, execute it and return the result set.")
                .parameters(objectSchema(props, required))
                .build();
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        Map<String, Object> args = codec.decode(request.getArguments());
        if (args == null) {
            args = Collections.emptyMap();
        }
        AskDataResult r = capability.ask(AskDataInput.builder()
                .question(str(args.get("question")))
                .build());
        return codec.encode(r);
    }

    private static Map<String, Object> stringProp(String d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", "string");
        m.put("description", d);
        return m;
    }

    private static Map<String, Object> objectSchema(Map<String, Object> props, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", required);
        return schema;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

}
