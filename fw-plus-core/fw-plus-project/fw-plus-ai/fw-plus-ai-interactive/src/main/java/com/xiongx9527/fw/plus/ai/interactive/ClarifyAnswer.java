package com.xiongx9527.fw.plus.ai.interactive;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户回填反问帧的应答（对应文档 §6.32.3 的 {@code clarify_answer} 帧）。
 *
 * <p>{@code clarifyId} 必须带回原反问帧的 {@code id}（防止多轮并发答错问题）；
 * {@code values} 为槽位名 → 提交值（选项用 {@code id}，自由输入用文本）。</p>
 */
public final class ClarifyAnswer {

    private final String clarifyId;
    private final Map<String, Object> values;

    private ClarifyAnswer(Builder builder) {
        this.clarifyId = builder.clarifyId;
        this.values = builder.values == null ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(builder.values);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ClarifyAnswer of(String clarifyId, Map<String, Object> values) {
        return builder().clarifyId(clarifyId).values(values).build();
    }

    public String getClarifyId() {
        return clarifyId;
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public static final class Builder {

        private String clarifyId;
        private Map<String, Object> values;

        public Builder clarifyId(String value) {
            this.clarifyId = value;
            return this;
        }

        public Builder values(Map<String, Object> value) {
            this.values = value;
            return this;
        }

        public ClarifyAnswer build() {
            return new ClarifyAnswer(this);
        }
    }
}
