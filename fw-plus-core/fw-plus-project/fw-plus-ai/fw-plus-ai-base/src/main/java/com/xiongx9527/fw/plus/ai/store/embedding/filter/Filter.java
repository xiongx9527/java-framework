package com.xiongx9527.fw.plus.ai.store.embedding.filter;

import com.xiongx9527.fw.plus.ai.store.embedding.filter.logical.And;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.logical.Not;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.logical.Or;

/**
 * 元数据过滤器：在向量检索时按元数据条件筛选。
 *
 * <p><b>作用</b>：向量相似度只解决「像不像」，业务还需要「只能查这个租户 / 这张表」。
 * 过滤器用可组合的表达式描述这类条件，由向量库实现下推到检索语句（而不是召回后再过滤）。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.store.embedding.filter.Filter}
 * （{@code test} + 实例 {@code and/or} + 静态 {@code and/or/not}）。</p>
 *
 * <p><b>实现约定</b>：不可变、可组合；{@link #test(Object)} 用于内存态实现，
 * 服务端下推的实现可只解析结构而不真正调用它。</p>
 *
 * @author xiongxiao
 */
public interface Filter {

    /**
     * 判断元数据值是否满足条件。
     *
     * @param value 待判定的元数据值
     * @return 满足返回 {@code true}
     */
    boolean test(Object value);

    /**
     * 与另一个过滤器取交集。
     *
     * @param other 另一个过滤器
     * @return 组合过滤器
     */
    default Filter and(Filter other) {
        return new And(this, other);
    }

    /**
     * 两个过滤器取交集。
     *
     * @param left  左
     * @param right 右
     * @return 组合过滤器
     */
    static Filter and(Filter left, Filter right) {
        return new And(left, right);
    }

    /**
     * 与另一个过滤器取并集。
     *
     * @param other 另一个过滤器
     * @return 组合过滤器
     */
    default Filter or(Filter other) {
        return new Or(this, other);
    }

    /**
     * 两个过滤器取并集。
     *
     * @param left  左
     * @param right 右
     * @return 组合过滤器
     */
    static Filter or(Filter left, Filter right) {
        return new Or(left, right);
    }

    /**
     * 取反。
     *
     * @param expression 原过滤器
     * @return 取反后的过滤器
     */
    static Filter not(Filter expression) {
        return new Not(expression);
    }
}
