package com.xiongx9527.fw.plus.ai.support.intent;

import com.xiongx9527.fw.plus.ai.base.intent.IntentContext;
import java.util.function.Predicate;

/**
 * 声明式意图规则（规范）的内置实现：把 {@code ref + 条件 + 分值 + 描述} 收成一条 {@link IntentRule}。
 *
 * <p>条件用 {@link Predicate}{@code <IntentContext>} 表达，可以组合文本与结构化属性；
 * 也提供了几个常用静态工厂（{@link #textContains}、{@link #attrEquals}）覆盖大多数“规则引擎”场景。</p>
 */
public final class IntentRuleSpec implements IntentRule {

    private final String ref;

    private final double score;

    private final String description;

    private final Predicate<IntentContext> when;

    private IntentRuleSpec(Builder b) {
        this.ref = b.ref;
        this.score = b.score;
        this.description = b.description;
        this.when = b.when;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public double getScore() {
        return score;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean test(IntentContext ctx) {
        return when != null && when.test(ctx);
    }

    /** 声明一条规则，从引用开始。 */
    public static Builder spec(String ref) {
        return new Builder(ref);
    }

    /** 文本包含关键词即命中（大小写不敏感）。 */
    public static IntentRuleSpec textContains(String ref, String keyword) {
        return spec(ref).score(1.0).description("text contains " + keyword)
                .when(new Predicate<IntentContext>() {
                    @Override
                    public boolean test(IntentContext ctx) {
                        return ctx.getTextLower().contains(keyword.toLowerCase());
                    }
                }).build();
    }

    /** 结构化属性等于期望值即命中。 */
    public static IntentRuleSpec attrEquals(String ref, String key, String expected) {
        return spec(ref).score(1.0).description(key + " == " + expected)
                .when(new Predicate<IntentContext>() {
                    @Override
                    public boolean test(IntentContext ctx) {
                        return ctx.attrEquals(key, expected);
                    }
                }).build();
    }

    public static final class Builder {

        private final String ref;

        private double score = 1.0;

        private String description = "";

        private Predicate<IntentContext> when;

        private Builder(String ref) {
            if (ref == null || ref.isEmpty()) {
                throw new IllegalArgumentException("intent rule ref is required");
            }
            this.ref = ref;
        }

        public Builder score(double value) {
            this.score = value;
            return this;
        }

        public Builder description(String value) {
            this.description = value == null ? "" : value;
            return this;
        }

        public Builder when(Predicate<IntentContext> value) {
            this.when = value;
            return this;
        }

        public IntentRuleSpec build() {
            if (when == null) {
                throw new IllegalStateException("intent rule requires a condition (when)");
            }
            return new IntentRuleSpec(this);
        }
    }
}
