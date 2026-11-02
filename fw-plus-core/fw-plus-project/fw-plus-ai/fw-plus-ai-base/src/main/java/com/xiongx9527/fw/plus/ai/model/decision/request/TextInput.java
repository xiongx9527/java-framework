package com.xiongx9527.fw.plus.ai.model.decision.request;

import java.util.Objects;

/**
 * 文本型决策输入。
 *
 * <p><b>作用</b>：最常见的输入形态，直接把一段文本作为决策依据。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.TextInput}。</p>
 *
 * @author xiongxiao
 */
public final class TextInput implements DecisionInput {

    private final String text;

    private TextInput(String text) {
        this.text = text;
    }

    /**
     * 由文本创建。
     *
     * @param text 文本
     * @return 输入对象
     */
    public static TextInput from(String text) {
        return new TextInput(text);
    }

    /**
     * 文本。
     *
     * @return 文本
     */
    public String text() {
        return text;
    }

    @Override
    public DecisionInputType type() {
        return DecisionInputType.TEXT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TextInput)) {
            return false;
        }
        return Objects.equals(text, ((TextInput) o).text);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text);
    }

    @Override
    public String toString() {
        return "TextInput{text='" + text + "'}";
    }
}
