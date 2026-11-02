package com.xiongx9527.fw.plus.ai.askdata.prompt;

/** 构造后的提示词（system + user）。 */
public class SqlPrompt {
    private final String systemPrompt;
    private final String userPrompt;

    public SqlPrompt(String systemPrompt, String userPrompt) {
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }
}
