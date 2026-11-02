package com.xiongx9527.fw.plus.ai.store.embedding.filter.logical;

import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;

/**
 * 逻辑非：条件取反。
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.store.embedding.filter.logical.Not}。</p>
 *
 * @author xiongxiao
 */
public class Not implements Filter {

    private final Filter expression;

    /**
     * 构造。
     *
     * @param expression 原条件
     */
    public Not(Filter expression) {
        this.expression = expression;
    }

    /**
     * 原条件。
     *
     * @return 原条件
     */
    public Filter expression() {
        return expression;
    }

    @Override
    public boolean test(Object value) {
        return !expression.test(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Not)) {
            return false;
        }
        Not that = (Not) o;
        return java.util.Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(expression);
    }

    @Override
    public String toString() {
        return "NOT (" + expression + ")";
    }
}
