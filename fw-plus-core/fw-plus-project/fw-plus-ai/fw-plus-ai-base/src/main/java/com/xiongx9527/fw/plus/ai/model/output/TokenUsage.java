package com.xiongx9527.fw.plus.ai.model.output;

/**
 * 令牌用量。
 *
 * <p><b>作用</b>：记录一次模型调用的输入 / 输出令牌数，用于计费、配额与上下文窗口估算。
 * 三个计数均为可空 {@link Integer}：厂商未返回时保持 {@code null}，与「返回 0」语义不同。</p>
 *
 * <p><b>出处</b>：逐字镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.output.TokenUsage}。
 * Spring AI 等价 {@code org.springframework.ai.chat.metadata.Usage}。</p>
 *
 * <p><b>实现约定</b>：不可变对象；{@link #totalTokenCount()} 在输入或输出任一为 {@code null} 时返回 {@code null}，
 * 不做「null 当 0」的兜底，避免把「未知」误报成「零消耗」。</p>
 */
public class TokenUsage {

    private final Integer inputTokenCount;
    private final Integer outputTokenCount;
    private final Integer totalTokenCount;

    /**
     * 创建一个所有计数均为 {@code null} 的用量对象（表示「未知」）。
     */
    public TokenUsage() {
        this(null, null, null);
    }

    /**
     * 只已知总令牌数时使用。
     *
     * @param totalTokenCount 总令牌数，可为 {@code null}
     */
    public TokenUsage(Integer totalTokenCount) {
        this(null, null, totalTokenCount);
    }

    /**
     * 已知输入与输出令牌数时使用，总量按两者之和推导。
     *
     * @param inputTokenCount  输入令牌数，可为 {@code null}
     * @param outputTokenCount 输出令牌数，可为 {@code null}
     */
    public TokenUsage(Integer inputTokenCount, Integer outputTokenCount) {
        this(inputTokenCount, outputTokenCount, sum(inputTokenCount, outputTokenCount));
    }

    /**
     * 三项均已知（或需要覆盖推导值）时使用。
     *
     * @param inputTokenCount  输入令牌数，可为 {@code null}
     * @param outputTokenCount 输出令牌数，可为 {@code null}
     * @param totalTokenCount  总令牌数，可为 {@code null}
     */
    public TokenUsage(Integer inputTokenCount, Integer outputTokenCount, Integer totalTokenCount) {
        this.inputTokenCount = inputTokenCount;
        this.outputTokenCount = outputTokenCount;
        this.totalTokenCount = totalTokenCount;
    }

    /**
     * 返回输入令牌数。
     *
     * @return 输入令牌数；厂商未返回时为 {@code null}
     */
    public Integer inputTokenCount() {
        return inputTokenCount;
    }

    /**
     * 返回输出令牌数。
     *
     * @return 输出令牌数；厂商未返回时为 {@code null}
     */
    public Integer outputTokenCount() {
        return outputTokenCount;
    }

    /**
     * 返回总令牌数。
     *
     * @return 总令牌数；未知时为 {@code null}
     */
    public Integer totalTokenCount() {
        return totalTokenCount;
    }

    /**
     * 累加两份用量，任一侧为 {@code null} 时返回另一侧。
     *
     * @param first  第一份用量，可为 {@code null}
     * @param second 第二份用量，可为 {@code null}
     * @return 累加结果；两者皆为 {@code null} 时返回 {@code null}
     */
    public static TokenUsage sum(TokenUsage first, TokenUsage second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return new TokenUsage(
                sum(first.inputTokenCount, second.inputTokenCount),
                sum(first.outputTokenCount, second.outputTokenCount),
                sum(first.totalTokenCount, second.totalTokenCount));
    }

    /**
     * 累加另一份用量。
     *
     * @param that 另一份用量，可为 {@code null}
     * @return 累加结果；{@code that} 为 {@code null} 时返回 {@code this}
     */
    public TokenUsage add(TokenUsage that) {
        return sum(this, that);
    }

    /**
     * 对可空计数做加法，任一侧为 {@code null} 时结果为 {@code null}。
     *
     * @param first  第一个计数，可为 {@code null}
     * @param second 第二个计数，可为 {@code null}
     * @return 两数之和；任一为 {@code null} 时为 {@code null}
     */
    protected static Integer sum(Integer first, Integer second) {
        if (first == null || second == null) {
            return null;
        }
        return first + second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TokenUsage)) {
            return false;
        }
        TokenUsage that = (TokenUsage) o;
        return java.util.Objects.equals(inputTokenCount, that.inputTokenCount)
                && java.util.Objects.equals(outputTokenCount, that.outputTokenCount)
                && java.util.Objects.equals(totalTokenCount, that.totalTokenCount);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(inputTokenCount, outputTokenCount, totalTokenCount);
    }

    @Override
    public String toString() {
        return "TokenUsage{inputTokenCount=" + inputTokenCount
                + ", outputTokenCount=" + outputTokenCount
                + ", totalTokenCount=" + totalTokenCount + "}";
    }
}
