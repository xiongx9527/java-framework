package com.xiongx9527.fw.plus.ai.askdata.capability;

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
 * askdata.generate_sql tool: lets an agentic loop (AiServices) ask the NL2SQL capability to produce SQL.
 * The tool spec is hand-written JSON Schema; arguments are decoded by the injected codec and the
 * SchemaContext is rebuilt from the decoded Map (no JSON library in this module).
 */
public class SqlGenToolProvider implements ToolProvider {

    public static final String TOOL_NAME = "askdata.generate_sql";

    private final SqlGenerationCapability capability;
    private final ToolArgumentCodec codec;

    public SqlGenToolProvider(SqlGenerationCapability capability, ToolArgumentCodec codec) {
        if (capability == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.gen_capability_required"));
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
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("question", question);
        props.put("schemaContext", schemaCtx);
        props.put("dialectHint", dialect);
        props.put("priorSql", prior);
        props.put("refResolution", ref);
        props.put("seed", seed);
        props.put("temperature", temp);
        props.put("topP", topp);
        List<String> required = new ArrayList<String>();
        required.add("question");
        required.add("schemaContext");
        return ToolSpecification.builder()
                .name(TOOL_NAME)
                .description("Generate executable SQL from a natural-language question against the provided schema context.")
                .parameters(objectSchema(props, required))
                .build();
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        if (request == null || request.getName() == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.tool_name_required"));
        }
        Map<String, Object> args = codec.decode(request.getArguments());
        if (args == null) {
            args = Collections.emptyMap();
        }
        SqlGenInput.Builder b = SqlGenInput.builder()
                .question(str(args.get("question")))
                .schemaContext(SchemaContexts.fromMap(args.get("schemaContext")))
                .dialectHint(str(args.get("dialectHint")))
                .priorSql(str(args.get("priorSql")))
                .refResolution(str(args.get("refResolution")))
                .seed(intOrNull(args.get("seed")))
                .temperature(doubleOrNull(args.get("temperature")))
                .topP(doubleOrNull(args.get("topP")));
        SqlGenResult r = capability.generate(b.build());
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
}
