package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.base.tool.ToolArgumentCodec;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContexts;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.ArrayList;
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
        Map<String, Object> schemaCtx = objectProp("schema context: tables[name,comment,columns[name,type,comment,primaryKey,nullable],primaryKey,relations[fromTable,fromColumn,toTable,toColumn]]");
        Map<String, Object> dialect = stringProp("sql dialect hint, e.g. mysql/postgresql");
        Map<String, Object> prior = stringProp("prior SQL from multi-turn context");
        Map<String, Object> ref = stringProp("reference resolution note");
        Map<String, Object> seed = integerProp("random seed for deterministic generation");
        Map<String, Object> temp = numberProp("sampling temperature");
        Map<String, Object> topp = numberProp("nucleus sampling probability");
        Map<String, Object> maxRows = integerProp("max rows to return (default 1000)");
        Map<String, Object> timeout = integerProp("query timeout seconds (default 30)");
        Map<String, Object> readOnly = boolProp("read-only guard (default true)");
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("question", question);
        props.put("schemaContext", schemaCtx);
        props.put("dialectHint", dialect);
        props.put("priorSql", prior);
        props.put("refResolution", ref);
        props.put("seed", seed);
        props.put("temperature", temp);
        props.put("topP", topp);
        props.put("maxRows", maxRows);
        props.put("queryTimeoutSeconds", timeout);
        props.put("readOnly", readOnly);
        List<String> required = new ArrayList<String>();
        required.add("question");
        required.add("schemaContext");
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
        SchemaContext schema = SchemaContexts.fromMap(args.get("schemaContext"));
        Integer maxRows = intOrNull(args.get("maxRows"));
        Integer timeout = intOrNull(args.get("queryTimeoutSeconds"));
        Boolean ro = boolOrNull(args.get("readOnly"));
        AskDataInput.Builder b = AskDataInput.builder()
                .question(str(args.get("question")))
                .candidates(schema.getTables())
                .dialectHint(str(args.get("dialectHint")))
                .priorSql(str(args.get("priorSql")))
                .refResolution(str(args.get("refResolution")))
                .seed(intOrNull(args.get("seed")))
                .temperature(doubleOrNull(args.get("temperature")))
                .topP(doubleOrNull(args.get("topP")))
                .maxRows(maxRows == null ? 1000 : maxRows)
                .queryTimeoutSeconds(timeout == null ? 30 : timeout)
                .readOnly(ro == null ? true : ro);
        AskDataResult r = capability.ask(b.build());
        return codec.encode(r);
    }

    private static Map<String, Object> stringProp(String d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", "string");
        m.put("description", d);
        return m;
    }

    private static Map<String, Object> objectProp(String d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", "object");
        m.put("description", d);
        return m;
    }

    private static Map<String, Object> integerProp(String d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", "integer");
        m.put("description", d);
        return m;
    }

    private static Map<String, Object> numberProp(String d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", "number");
        m.put("description", d);
        return m;
    }

    private static Map<String, Object> boolProp(String d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", "boolean");
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

    private static Integer intOrNull(Object o) {
        if (o == null) {
            return null;
        }
        return Integer.valueOf(String.valueOf(o));
    }

    private static Double doubleOrNull(Object o) {
        if (o == null) {
            return null;
        }
        return Double.valueOf(String.valueOf(o));
    }

    private static Boolean boolOrNull(Object o) {
        if (o == null) {
            return null;
        }
        return Boolean.valueOf(String.valueOf(o));
    }
}
