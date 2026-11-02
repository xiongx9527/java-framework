package com.xiongx9527.fw.plus.ai.support.context;

/**
 * 端口错误：跨后端统一的失败描述，含「能不能重试」与「用户该做什么」。
 *
 * <p><b>作用</b>：不同后端抛出的异常类型五花八门（HTTP 429、超时、鉴权失败、内容审查、模型不存在…）。
 * 上层（问数主链、前端提示、熔断与故障转移）需要的其实只有三件事：错误码、是否可重试、给用户的建议动作。
 * 本类型就是这三件事的载体，配合框架的 {@code BizException} 错误码体系（8 位：来源 + 类别 + 序号）使用。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.40.4 公共 DTO，
 * 以及增强清单「进度反馈与失败可行动（错误码 + 建议动作）」。langchain4j 无等价类型（只有分散的异常类）。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #code()} 遵循框架 8 位错误码约定；{@link #retryable()} 为 {@code true} 时
 * 调用方可以原样重试，否则必须换模型 / 换参数 / 找人工。</p>
 *
 * @author xiongxiao
 */
public final class PortError {

    private final int code;
    private final String category;
    private final String message;
    private final String reason;
    private final boolean retryable;
    private final String suggestion;
    private final Throwable cause;

    private PortError(Builder builder) {
        this.code = builder.code;
        this.category = builder.category;
        this.message = builder.message;
        this.reason = builder.reason;
        this.retryable = builder.retryable;
        this.suggestion = builder.suggestion;
        this.cause = builder.cause;
    }

    /** @return 框架 8 位错误码 */
    public int code() { return code; }

    /** @return 错误类别（如 {@code CONFIG} / {@code MODEL} / {@code QUOTA}），用于聚合告警 */
    public String category() { return category; }

    /** @return 面向用户的消息 key 或文案，由上层做 i18n */
    public String message() { return message; }

    /** @return 原始技术原因，用于日志与排查，不直接展示给用户 */
    public String reason() { return reason; }

    /** @return 是否可重试 */
    public boolean retryable() { return retryable; }

    /** @return 建议动作（如「稍后重试」「联系管理员充值」「换一个模型」） */
    public String suggestion() { return suggestion; }

    /** @return 原始异常，可能为 {@code null} */
    public Throwable cause() { return cause; }

    /**
     * 创建构建器。
     *
     * @return 空构建器
     */
    public static Builder builder() { return new Builder(); }

    /** {@link PortError} 构建器。 */
    public static final class Builder {
        private int code;
        private String category;
        private String message;
        private String reason;
        private boolean retryable;
        private String suggestion;
        private Throwable cause;

        public Builder code(int code) { this.code = code; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder retryable(boolean retryable) { this.retryable = retryable; return this; }
        public Builder suggestion(String suggestion) { this.suggestion = suggestion; return this; }
        public Builder cause(Throwable cause) { this.cause = cause; return this; }
        public PortError build() { return new PortError(this); }
    }
}
