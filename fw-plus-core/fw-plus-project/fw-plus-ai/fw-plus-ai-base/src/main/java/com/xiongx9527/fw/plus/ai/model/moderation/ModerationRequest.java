package com.xiongx9527.fw.plus.ai.model.moderation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 内容审核请求。
 *
 * <p><b>作用</b>：一次提交多段待审文本，由模型判断是否违规；批量形态可减少往返。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.moderation.ModerationRequest}。</p>
 *
 * <p><b>实现约定</b>：{@link #texts()} 不可变且非空。</p>
 *
 * @author xiongxiao
 */
public class ModerationRequest {

    private final List<String> texts;
    private final String modelName;

    private ModerationRequest(Builder builder) {
        if (builder.texts == null || builder.texts.isEmpty()) {
            throw new IllegalArgumentException("texts must not be empty");
        }
        this.texts = Collections.unmodifiableList(new ArrayList<String>(builder.texts));
        this.modelName = builder.modelName;
    }

    /**
     * 待审文本列表。
     *
     * @return 只读文本列表，非 {@code null}
     */
    public List<String> texts() {
        return texts;
    }

    /**
     * 模型名。
     *
     * @return 模型名；未指定时为 {@code null}
     */
    public String modelName() {
        return modelName;
    }

    /**
     * 基于当前请求创建构建器（便于局部修改）。
     *
     * @return 构建器
     */
    public Builder toBuilder() {
        return builder().texts(texts).modelName(modelName);
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModerationRequest)) {
            return false;
        }
        ModerationRequest that = (ModerationRequest) o;
        return Objects.equals(texts, that.texts) && Objects.equals(modelName, that.modelName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texts, modelName);
    }

    @Override
    public String toString() {
        return "ModerationRequest{texts=" + texts + ", modelName='" + modelName + "'}";
    }

    /** {@link ModerationRequest} 构建器。 */
    public static class Builder {

        private List<String> texts;
        private String modelName;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置待审文本列表。
         *
         * @param texts 文本列表
         * @return 本构建器
         */
        public Builder texts(List<String> texts) {
            this.texts = texts;
            return this;
        }

        /**
         * 设置模型名。
         *
         * @param modelName 模型名
         * @return 本构建器
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * 构建请求。
         *
         * @return 请求对象
         */
        public ModerationRequest build() {
            return new ModerationRequest(this);
        }
    }
}
