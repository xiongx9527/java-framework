package com.xiongx9527.fw.plus.ai.model.moderation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 内容审核响应。
 *
 * <p><b>作用</b>：返回审核结论，并保留厂商原始元数据（如各违规类别分数），供策略层二次判断。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.moderation.ModerationResponse}。</p>
 *
 * @author xiongxiao
 */
public class ModerationResponse {

    private final Moderation moderation;
    private final Map<String, Object> metadata;

    private ModerationResponse(Builder builder) {
        this.moderation = builder.moderation == null ? Moderation.notFlagged() : builder.moderation;
        this.metadata = builder.metadata == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.metadata));
    }

    /**
     * 审核结论。
     *
     * @return 审核结论，非 {@code null}
     */
    public Moderation moderation() {
        return moderation;
    }

    /**
     * 厂商原始元数据。
     *
     * @return 只读元数据 Map，非 {@code null}
     */
    public Map<String, Object> metadata() {
        return metadata;
    }

    /**
     * 基于当前响应创建构建器。
     *
     * @return 构建器
     */
    public Builder toBuilder() {
        return builder().moderation(moderation).metadata(metadata);
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
        if (!(o instanceof ModerationResponse)) {
            return false;
        }
        ModerationResponse that = (ModerationResponse) o;
        return Objects.equals(moderation, that.moderation) && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moderation, metadata);
    }

    @Override
    public String toString() {
        return "ModerationResponse{moderation=" + moderation + ", metadata=" + metadata + "}";
    }

    /** {@link ModerationResponse} 构建器。 */
    public static class Builder {

        private Moderation moderation;
        private Map<String, Object> metadata;

        /** 构造空构建器。 */
        public Builder() {
        }

        /**
         * 设置审核结论。
         *
         * @param moderation 审核结论
         * @return 本构建器
         */
        public Builder moderation(Moderation moderation) {
            this.moderation = moderation;
            return this;
        }

        /**
         * 设置厂商原始元数据。
         *
         * @param metadata 元数据 Map
         * @return 本构建器
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * 构建响应。
         *
         * @return 响应对象
         */
        public ModerationResponse build() {
            return new ModerationResponse(this);
        }
    }
}
