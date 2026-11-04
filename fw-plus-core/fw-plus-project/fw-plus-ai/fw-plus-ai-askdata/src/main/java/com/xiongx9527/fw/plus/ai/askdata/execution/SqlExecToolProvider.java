package com.xiongx9527.fw.plus.ai.askdata.execution;

import com.xiongx9527.fw.plus.ai.base.tool.ToolArgumentCodec;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * askdata.execute_sql tool: lets an agentic loop execute a read-only SQL statement and get the result set.
 */
public class SqlExecToolProvider implements ToolProvider {

    public static final String TOOL_NAME = "askdata.execute_sql";

    private final SqlExecutionCapability capability;
    private final ToolArgumentCodec codec;

    public SqlExecToolProvider(SqlExecutionCapability capability, ToolArgumentCodec codec) {
        if (capability == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.exec_capability_required"));
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
        Map<String, Object> sql = stringProp("read-only SQL to execute");
        Map<String, Object> dialect = stringProp("sql dialect hint");
        Map<String, Object> maxRows = integerProp("max rows to return (default 1000)");
        Map<String, Object> timeout = integerProp("query timeout seconds (default 30)");
        Map<String, Object> readOnly = boolProp("read-only guard (default true)");
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("sql", sql);
        props.put("dialectHint", dialect);
        props.put("maxRows", maxRows);
        props.put("queryTimeoutSeconds", timeout);
        props.put("readOnly", readOnly);
        List<String> required = new ArrayList<String>();
        required.add("sql");
        return ToolSpecification.builder()
                .name(TOOL_NAME)
                .description("Execute a read-only SQL statement and return the result set (columns + rows).")
                .parameters(objectSchema(props, required))
                .build();
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        Map<String, Object> args = codec.decode(request.getArguments());
        if (args == null) {
            args = Collections.emptyMap();
        }
        SqlExecInput.Builder b = SqlExecInput.builder().sql(str(args.get("sql")));
        b.dialectHint(str(args.get("dialectHint")));
        Integer mr = intOrNull(args.get("maxRows"));
        if (mr != null) {
            b.maxRows(mr);
        }
        Integer to = intOrNull(args.get("queryTimeoutSeconds"));
        if (to != null) {
            b.queryTimeoutSeconds(to);
        }
        Boolean ro = boolOrNull(args.get("readOnly"));
        if (ro != null) {
            b.readOnly(ro);
        }
        QueryResult qr = capability.execute(b.build());
        return codec.encode(resultToMap(qr));
    }

    private static Map<String, Object> resultToMap(QueryResult qr) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> cols = new ArrayList<Map<String, Object>>();
        for (ColumnMeta c : qr.getColumns()) {
            Map<String, Object> cm = new LinkedHashMap<String, Object>();
            cm.put("name", c.getName());
            cm.put("type", c.getType());
            cols.add(cm);
        }
        m.put("columns", cols);
        m.put("rows", qr.getRows());
        m.put("rowCount", qr.rowCount());
        m.put("truncated", qr.isTruncated());
        m.put("warnings", qr.getWarnings());
        return m;
    }

    private static Map<String, Object> stringProp(String d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", "string");
        m.put("description", d);
        return m;
    }

    private static Map<String, Object> integerProp(String d) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", "integer");
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

    private static Boolean boolOrNull(Object o) {
        if (o == null) {
            return null;
        }
        return Boolean.valueOf(String.valueOf(o));
    }
}
