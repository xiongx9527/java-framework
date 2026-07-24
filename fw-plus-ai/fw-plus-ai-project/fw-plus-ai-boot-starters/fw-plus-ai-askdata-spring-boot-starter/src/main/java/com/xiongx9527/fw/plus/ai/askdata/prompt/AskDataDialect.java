package com.xiongx9527.fw.plus.ai.askdata.prompt;

public class AskDataDialect {

    private final String name;
    private final String identifierRule;
    private final String limitRule;

    public AskDataDialect(String name, String identifierRule, String limitRule) {
        this.name = name;
        this.identifierRule = identifierRule;
        this.limitRule = limitRule;
    }

    public String getName() { return name; }
    public String getIdentifierRule() { return identifierRule; }
    public String getLimitRule() { return limitRule; }
}
