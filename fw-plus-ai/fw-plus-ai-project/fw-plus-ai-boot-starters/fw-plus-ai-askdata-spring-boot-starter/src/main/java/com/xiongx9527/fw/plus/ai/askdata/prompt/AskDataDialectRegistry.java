package com.xiongx9527.fw.plus.ai.askdata.prompt;

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

    public AskDataDialectRegistry() {
        dialects.put("mysql", new AskDataDialect("mysql", "标识符使用反引号 (`) 包裹。", "使用 LIMIT 限制返回行数。",
                STANDARD_LIMIT_APPENDER));
        dialects.put("postgresql", new AskDataDialect("postgresql", "标识符使用双引号 (\") 包裹。", "使用 LIMIT 限制返回行数。",
                STANDARD_LIMIT_APPENDER));
        dialects.put("clickhouse", new AskDataDialect("clickhouse", "标识符使用反引号 (`) 包裹。", "使用 LIMIT 限制返回行数。",
                STANDARD_LIMIT_APPENDER));
    }

    public AskDataDialect getDialect(String dialect) {
        if (isBlank(dialect)) {
            return dialects.get("mysql");
        }
        AskDataDialect result = dialects.get(dialect.trim().toLowerCase());
        return result == null ? new AskDataDialect(dialect.trim(), "按 " + dialect.trim() + " 的标识符规则处理。",
                "使用 LIMIT 限制返回行数。", STANDARD_LIMIT_APPENDER) : result;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
