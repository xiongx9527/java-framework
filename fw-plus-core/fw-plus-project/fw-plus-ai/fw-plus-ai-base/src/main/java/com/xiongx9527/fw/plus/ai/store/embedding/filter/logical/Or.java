package com.xiongx9527.fw.plus.ai.store.embedding.filter.logical;

import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;

/**
 * 逻辑或：两个条件任一满足。
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.store.embedding.filter.logical.Or}。</p>
 *
 * @author xiongxiao
 */
public class Or implements Filter {

    private final Filter left;
    private final Filter right;

    /**
     * 构造。
     *
     * @param left  左条件
     * @param right 右条件
     */
    public Or(Filter left, Filter right) {
        this.left = left;
        this.right = right;
    }

    /**
     * 左条件。
     *
     * @return 左条件
     */
    public Filter left() {
        return left;
    }

    /**
     * 右条件。
     *
     * @return 右条件
     */
    public Filter right() {
        return right;
    }

    @Override
    public boolean test(Object value) {
        return left.test(value) || right.test(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Or)) {
            return false;
        }
        Or that = (Or) o;
        return java.util.Objects.equals(left, that.left)
                && java.util.Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "(" + left + " OR " + right + ")";
    }
}
