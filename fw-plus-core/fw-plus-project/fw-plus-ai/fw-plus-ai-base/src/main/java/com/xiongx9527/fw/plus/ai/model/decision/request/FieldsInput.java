package com.xiongx9527.fw.plus.ai.model.decision.request;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 字段型决策输入。
 *
 * <p><b>作用</b>：以「键 → 值」的结构化字段作为决策依据，便于把数据库记录 / 表单直接喂给决策模型。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.FieldsInput}。</p>
 *
 * @author xiongxiao
 */
public final class FieldsInput implements DecisionInput {

    private final Map<String, Object> fields;

    private FieldsInput(Map<String, ?> fields) {
        this.fields = Collections.unmodifiableMap(new java.util.LinkedHashMap<String, Object>(fields));
    }

    /**
     * 由字段映射创建。
     *
     * @param fields 字段映射
     * @return 输入对象
     */
    public static FieldsInput from(Map<String, ?> fields) {
        return new FieldsInput(fields);
    }

    /**
     * 字段映射。
     *
     * @return 只读 Map，非 {@code null}
     */
    public Map<String, Object> fields() {
        return fields;
    }

    @Override
    public DecisionInputType type() {
        return DecisionInputType.FIELDS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FieldsInput)) {
            return false;
        }
        return Objects.equals(fields, ((FieldsInput) o).fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
    }

    @Override
    public String toString() {
        return "FieldsInput{fields=" + fields + "}";
    }
}
