package com.xiongx9527.fw.plus.ai.template;

import java.util.Collections;
import java.util.Map;

/** 提示词模板（{{key}} 占位替换），镜像 langchain4j 的 {@code PromptTemplate}（Spring AI 等价 {@code PromptTemplate}）。 */
public class PromptTemplate {
    private final String template;
    public PromptTemplate(String template) { this.template = template; }
    public Prompt render() { return render(Collections.<String, Object>emptyMap()); }
    public Prompt render(Map<String, Object> variables) {
        String s = template;
        if (variables != null) {
            for (Map.Entry<String, Object> e : variables.entrySet()) {
                s = s.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue().toString());
            }
        }
        return new Prompt(s);
    }
    public static PromptTemplate from(String template) { return new PromptTemplate(template); }
}
