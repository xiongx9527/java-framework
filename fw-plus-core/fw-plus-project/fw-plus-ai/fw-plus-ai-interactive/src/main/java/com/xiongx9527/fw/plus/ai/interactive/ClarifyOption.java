package com.xiongx9527.fw.plus.ai.interactive;

/**
 * 反问帧 / 选择题里的一个选项。
 *
 * <p>选项必须可校验（提交用 {@code id}，前端展示用 {@code label}）；{@code validUntil} 过期后
 * 视为不可用，防止用户选到已经被业务侧取消的时段 / 枚举值。</p>
 */
public final class ClarifyOption {

    private final String id;
    private final String label;
    private final String meta;
    private final long validUntil;

    private ClarifyOption(Builder builder) {
        this.id = builder.id;
        this.label = builder.label;
        this.meta = builder.meta;
        this.validUntil = builder.validUntil;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getMeta() {
        return meta;
    }

    public long getValidUntil() {
        return validUntil;
    }

    public boolean isExpired() {
        return validUntil > 0 && validUntil < System.currentTimeMillis();
    }

    public static final class Builder {

        private String id;
        private String label;
        private String meta;
        private long validUntil;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder label(String value) {
            this.label = value;
            return this;
        }

        public Builder meta(String value) {
            this.meta = value;
            return this;
        }

        public Builder validUntil(long value) {
            this.validUntil = value;
            return this;
        }

        public ClarifyOption build() {
            if (id == null) {
                throw new IllegalStateException("ClarifyOption.id is required");
            }
            return new ClarifyOption(this);
        }
    }
}
