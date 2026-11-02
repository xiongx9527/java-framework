package com.xiongx9527.fw.plus.ai.support.context;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次调用的横切上下文：谁在调、调什么、还剩多少预算。
 *
 * <p><b>作用</b>：模型调用链路上很多能力（配额、成本、缓存、幂等、审计、灰度）都需要同一组「调用身份」信息。
 * 与其在每层方法签名里散着传 traceId / userId / chatId，不如收成一个不可变上下文对象随调用透传。
 * 它<b>不承载业务语义</b>——问题文本、SQL、数据集这些属于领域入参，不进这里。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.40.4 的公共 DTO，
 * 服务于增强清单中的成本核算、配额限流、幂等、全链路日志等条目；langchain4j 与 Spring AI 均无等价类型
 * （langchain4j 的 {@code InvocationParameters} 只作用于 RAG 检索）。</p>
 *
 * <p><b>实现约定</b>：不可变 + 线程安全；{@link #attributes()} 只读；下游若需追加信息，用 {@link #toBuilder()} 派生新实例。</p>
 *
 * @author xiongxiao
 */
public final class CallContext {

    private final String traceId;
    private final String spanId;
    private final String tenantId;
    private final String userId;
    private final String chatId;
    private final String modelId;
    private final String locale;
    private final Duration timeout;
    private final Long tokenBudget;
    private final Map<String, Object> attributes;

    private CallContext(Builder builder) {
        this.traceId = builder.traceId;
        this.spanId = builder.spanId;
        this.tenantId = builder.tenantId;
        this.userId = builder.userId;
        this.chatId = builder.chatId;
        this.modelId = builder.modelId;
        this.locale = builder.locale;
        this.timeout = builder.timeout;
        this.tokenBudget = builder.tokenBudget;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.attributes));
    }

    /** @return 链路追踪 ID，用于串联同一次请求的全部日志 */
    public String traceId() { return traceId; }

    /** @return 当前 span ID */
    public String spanId() { return spanId; }

    /** @return 租户 / 组织标识（配额与成本按此归集） */
    public String tenantId() { return tenantId; }

    /** @return 发起调用的用户标识 */
    public String userId() { return userId; }

    /** @return 会话标识（对话记忆、缓存键的组成部分） */
    public String chatId() { return chatId; }

    /** @return 本次调用使用的模型标识 */
    public String modelId() { return modelId; }

    /** @return 回答语言区域，如 {@code zh-CN} */
    public String locale() { return locale; }

    /** @return 调用超时，未设置时为 {@code null} */
    public Duration timeout() { return timeout; }

    /** @return token 预算上限，未设置时为 {@code null} */
    public Long tokenBudget() { return tokenBudget; }

    /** @return 只读扩展属性，永不为 {@code null} */
    public Map<String, Object> attributes() { return attributes; }

    /**
     * 读取扩展属性。
     *
     * @param key 属性键
     * @return 属性值，不存在时为 {@code null}
     */
    public Object attribute(String key) { return attributes.get(key); }

    /**
     * 以当前实例为模板派生新上下文。
     *
     * @return 已填充当前字段的构建器
     */
    public Builder toBuilder() {
        return new Builder()
                .traceId(traceId).spanId(spanId).tenantId(tenantId).userId(userId).chatId(chatId)
                .modelId(modelId).locale(locale).timeout(timeout).tokenBudget(tokenBudget)
                .attributes(attributes);
    }

    /**
     * 创建构建器。
     *
     * @return 空构建器
     */
    public static Builder builder() { return new Builder(); }

    /** {@link CallContext} 构建器，全部字段可选。 */
    public static final class Builder {
        private String traceId;
        private String spanId;
        private String tenantId;
        private String userId;
        private String chatId;
        private String modelId;
        private String locale;
        private Duration timeout;
        private Long tokenBudget;
        private Map<String, Object> attributes = new LinkedHashMap<String, Object>();

        public Builder traceId(String traceId) { this.traceId = traceId; return this; }
        public Builder spanId(String spanId) { this.spanId = spanId; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder chatId(String chatId) { this.chatId = chatId; return this; }
        public Builder modelId(String modelId) { this.modelId = modelId; return this; }
        public Builder locale(String locale) { this.locale = locale; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public Builder tokenBudget(Long tokenBudget) { this.tokenBudget = tokenBudget; return this; }
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(attributes);
            return this;
        }
        public Builder attribute(String key, Object value) { this.attributes.put(key, value); return this; }
        public CallContext build() { return new CallContext(this); }
    }
}
