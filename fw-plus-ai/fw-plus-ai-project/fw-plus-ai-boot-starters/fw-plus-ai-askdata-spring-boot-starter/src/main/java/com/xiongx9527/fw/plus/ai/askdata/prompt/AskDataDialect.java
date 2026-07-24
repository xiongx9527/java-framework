package com.xiongx9527.fw.plus.ai.askdata.prompt;

public class AskDataDialect {

    private final String name;
    private final String identifierRule;
    private final String limitRule;
    private final LimitAppender limitAppender;

    public AskDataDialect(String name, String identifierRule, String limitRule) {
        this(name, identifierRule, limitRule, new LimitAppender() {
            @Override
            public String apply(String sql, int maxRows) {
                return sql + " limit " + maxRows;
            }
        });
    }

    public AskDataDialect(String name, String identifierRule, String limitRule, LimitAppender limitAppender) {
        this.name = name;
        this.identifierRule = identifierRule;
        this.limitRule = limitRule;
        this.limitAppender = limitAppender;
    }

    public String getName() { return name; }
    public String getIdentifierRule() { return identifierRule; }
    public String getLimitRule() { return limitRule; }

    public String applyLimit(String sql, int maxRows) {
        return limitAppender.apply(sql, maxRows);
    }

    public interface LimitAppender {
        String apply(String sql, int maxRows);
    }
}
