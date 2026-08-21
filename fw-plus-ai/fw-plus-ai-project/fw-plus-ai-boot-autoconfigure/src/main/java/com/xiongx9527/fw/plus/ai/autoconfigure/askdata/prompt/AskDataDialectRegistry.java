package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt;

import java.util.LinkedHashMap;
import java.util.Map;

public class AskDataDialectRegistry {

    private static final AskDataDialect.LimitAppender STANDARD_LIMIT_APPENDER = new AskDataDialect.LimitAppender() {
        @Override
        public String apply(String sql, int maxRows) {
            return sql + " limit " + maxRows;
        }
    };

    private final Map<String, AskDataDialect> dialects = new LinkedHashMap<String, AskDataDialect>();
    private final Map<String, AskDataDialectCapability> capabilities = new LinkedHashMap<String, AskDataDialectCapability>();

    public AskDataDialectRegistry() {
        register("mysql", "标识符使用反引号 (`) 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("oracle", "标识符使用双引号 (\") 包裹。", "使用 FETCH FIRST n ROWS ONLY 限制返回行数。", true);
        register("postgresql", "标识符使用双引号 (\") 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("sqlserver", "标识符可使用方括号 ([]) 包裹。", "使用 TOP (n) 或 OFFSET/FETCH 限制返回行数。", true);
        register("clickhouse", "标识符使用反引号 (`) 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("dameng", "标识符使用双引号 (\") 包裹。", "使用 FETCH FIRST n ROWS ONLY 限制返回行数。", true);
        register("doris", "标识符使用反引号 (`) 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("redshift", "标识符使用双引号 (\") 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("elasticsearch", "使用字段名和 Query DSL 描述。", "仅提供只读连接器描述，不生成 JDBC 执行 SQL。", false);
        register("kingbase", "标识符使用双引号 (\") 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("starrocks", "标识符使用反引号 (`) 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("hive", "标识符使用反引号 (`) 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("excel-csv", "使用表头字段名。", "仅提供只读文件连接器描述，不生成 JDBC 执行 SQL。", false);
        register("bigquery", "标识符使用反引号 (`) 包裹完整表路径。", "使用 LIMIT 限制返回行数。", true);
        register("duckdb", "标识符使用双引号 (\") 包裹。", "使用 LIMIT 限制返回行数。", true);
        register("snowflake", "标识符使用双引号 (\") 包裹。", "使用 LIMIT 限制返回行数。", true);
    }

    public AskDataDialect getDialect(String dialect) {
        if (isBlank(dialect)) {
            return dialects.get("mysql");
        }
        AskDataDialect result = dialects.get(dialect.trim().toLowerCase());
        return result == null ? new AskDataDialect(dialect.trim(), "按 " + dialect.trim() + " 的标识符规则处理。",
                "使用 LIMIT 限制返回行数。", STANDARD_LIMIT_APPENDER) : result;
    }

    public AskDataDialectCapability getCapability(String connector) {
        String key = isBlank(connector) ? "mysql" : connector.trim().toLowerCase();
        AskDataDialectCapability capability = capabilities.get(key);
        if (capability != null) {
            return capability;
        }
        return new AskDataDialectCapability(getDialect(connector), false, "未识别连接器；仅提供只读方言描述，必须由业务 SPI 显式接入执行器。");
    }

    private void register(String name, String identifierRule, String limitRule, boolean directExecutionSupported) {
        AskDataDialect dialect = new AskDataDialect(name, identifierRule, limitRule, STANDARD_LIMIT_APPENDER);
        dialects.put(name, dialect);
        capabilities.put(name, new AskDataDialectCapability(dialect, directExecutionSupported,
                directExecutionSupported ? "支持受控只读 SQL 执行；仍须通过 SQL 校验和宿主数据源授权。"
                        : "仅提供只读能力描述；当前 Starter 不直接执行该连接器。"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
