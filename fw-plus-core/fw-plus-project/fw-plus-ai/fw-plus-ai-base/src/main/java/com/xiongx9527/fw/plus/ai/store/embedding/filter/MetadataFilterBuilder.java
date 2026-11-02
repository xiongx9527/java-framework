package com.xiongx9527.fw.plus.ai.store.embedding.filter;

import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.ContainsString;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.IsEqualTo;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.IsGreaterThan;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.IsGreaterThanOrEqualTo;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.IsIn;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.IsLessThan;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.IsLessThanOrEqualTo;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.IsNotEqualTo;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison.IsNotIn;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.logical.And;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 元数据过滤条件构建器。
 *
 * <p><b>作用</b>：用流式写法表达「某键等于 / 不等于 / 大于 / 介于 / 属于」等条件，
 * 避免调用方直接 new 一堆 {@code IsGreaterThan} 之类难以阅读的类。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐方法镜像
 * {@code dev.langchain4j.store.embedding.filter.MetadataFilterBuilder}。</p>
 *
 * <p><b>实现约定</b>：实例只持有键名，可重复使用；每个方法返回新的 {@link Filter}，不修改自身状态。
 * {@code isBetween} 由「大于等于下界」与「小于等于上界」两个条件与运算组合而成。</p>
 *
 * @author xiongxiao
 */
public class MetadataFilterBuilder {

    private final String key;

    /**
     * 构造。
     *
     * @param key 元数据键，不能为空白
     */
    public MetadataFilterBuilder(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        this.key = key;
    }

    /**
     * 以元数据键创建构建器。
     *
     * @param key 元数据键
     * @return 构建器
     */
    public static MetadataFilterBuilder metadataKey(String key) {
        return new MetadataFilterBuilder(key);
    }

    /**
     * 字符串包含。
     *
     * @param value 子串
     * @return 过滤条件
     */
    public Filter containsString(String value) {
        return new ContainsString(key, value);
    }

    /**
     * 等于（字符串）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isEqualTo(String value) {
        return new IsEqualTo(key, value);
    }

    /**
     * 等于（UUID）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isEqualTo(UUID value) {
        return new IsEqualTo(key, value);
    }

    /**
     * 等于（int）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isEqualTo(int value) {
        return new IsEqualTo(key, value);
    }

    /**
     * 等于（long）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isEqualTo(long value) {
        return new IsEqualTo(key, value);
    }

    /**
     * 等于（float）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isEqualTo(float value) {
        return new IsEqualTo(key, value);
    }

    /**
     * 等于（double）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isEqualTo(double value) {
        return new IsEqualTo(key, value);
    }

    /**
     * 不等于（字符串）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isNotEqualTo(String value) {
        return new IsNotEqualTo(key, value);
    }

    /**
     * 不等于（UUID）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isNotEqualTo(UUID value) {
        return new IsNotEqualTo(key, value);
    }

    /**
     * 不等于（int）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isNotEqualTo(int value) {
        return new IsNotEqualTo(key, value);
    }

    /**
     * 不等于（long）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isNotEqualTo(long value) {
        return new IsNotEqualTo(key, value);
    }

    /**
     * 不等于（float）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isNotEqualTo(float value) {
        return new IsNotEqualTo(key, value);
    }

    /**
     * 不等于（double）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isNotEqualTo(double value) {
        return new IsNotEqualTo(key, value);
    }

    /**
     * 大于（字符串，按字典序）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThan(String value) {
        return new IsGreaterThan(key, value);
    }

    /**
     * 大于（int）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThan(int value) {
        return new IsGreaterThan(key, value);
    }

    /**
     * 大于（long）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThan(long value) {
        return new IsGreaterThan(key, value);
    }

    /**
     * 大于（float）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThan(float value) {
        return new IsGreaterThan(key, value);
    }

    /**
     * 大于（double）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThan(double value) {
        return new IsGreaterThan(key, value);
    }

    /**
     * 大于等于（字符串，按字典序）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThanOrEqualTo(String value) {
        return new IsGreaterThanOrEqualTo(key, value);
    }

    /**
     * 大于等于（int）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThanOrEqualTo(int value) {
        return new IsGreaterThanOrEqualTo(key, value);
    }

    /**
     * 大于等于（long）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThanOrEqualTo(long value) {
        return new IsGreaterThanOrEqualTo(key, value);
    }

    /**
     * 大于等于（float）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThanOrEqualTo(float value) {
        return new IsGreaterThanOrEqualTo(key, value);
    }

    /**
     * 大于等于（double）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isGreaterThanOrEqualTo(double value) {
        return new IsGreaterThanOrEqualTo(key, value);
    }

    /**
     * 小于（字符串，按字典序）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThan(String value) {
        return new IsLessThan(key, value);
    }

    /**
     * 小于（int）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThan(int value) {
        return new IsLessThan(key, value);
    }

    /**
     * 小于（long）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThan(long value) {
        return new IsLessThan(key, value);
    }

    /**
     * 小于（float）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThan(float value) {
        return new IsLessThan(key, value);
    }

    /**
     * 小于（double）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThan(double value) {
        return new IsLessThan(key, value);
    }

    /**
     * 小于等于（字符串，按字典序）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThanOrEqualTo(String value) {
        return new IsLessThanOrEqualTo(key, value);
    }

    /**
     * 小于等于（int）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThanOrEqualTo(int value) {
        return new IsLessThanOrEqualTo(key, value);
    }

    /**
     * 小于等于（long）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThanOrEqualTo(long value) {
        return new IsLessThanOrEqualTo(key, value);
    }

    /**
     * 小于等于（float）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThanOrEqualTo(float value) {
        return new IsLessThanOrEqualTo(key, value);
    }

    /**
     * 小于等于（double）。
     *
     * @param value 比较值
     * @return 过滤条件
     */
    public Filter isLessThanOrEqualTo(double value) {
        return new IsLessThanOrEqualTo(key, value);
    }

    /**
     * 区间（字符串，闭区间）。
     *
     * @param from 下界（含）
     * @param to   上界（含）
     * @return 过滤条件
     */
    public Filter isBetween(String from, String to) {
        return new And(new IsGreaterThanOrEqualTo(key, from), new IsLessThanOrEqualTo(key, to));
    }

    /**
     * 区间（int，闭区间）。
     *
     * @param from 下界（含）
     * @param to   上界（含）
     * @return 过滤条件
     */
    public Filter isBetween(int from, int to) {
        return new And(new IsGreaterThanOrEqualTo(key, from), new IsLessThanOrEqualTo(key, to));
    }

    /**
     * 区间（long，闭区间）。
     *
     * @param from 下界（含）
     * @param to   上界（含）
     * @return 过滤条件
     */
    public Filter isBetween(long from, long to) {
        return new And(new IsGreaterThanOrEqualTo(key, from), new IsLessThanOrEqualTo(key, to));
    }

    /**
     * 区间（float，闭区间）。
     *
     * @param from 下界（含）
     * @param to   上界（含）
     * @return 过滤条件
     */
    public Filter isBetween(float from, float to) {
        return new And(new IsGreaterThanOrEqualTo(key, from), new IsLessThanOrEqualTo(key, to));
    }

    /**
     * 区间（double，闭区间）。
     *
     * @param from 下界（含）
     * @param to   上界（含）
     * @return 过滤条件
     */
    public Filter isBetween(double from, double to) {
        return new And(new IsGreaterThanOrEqualTo(key, from), new IsLessThanOrEqualTo(key, to));
    }

    /**
     * 属于（字符串候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isIn(String... values) {
        return new IsIn(key, Arrays.<Object>asList(values));
    }

    /**
     * 属于（UUID 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isIn(UUID... values) {
        return new IsIn(key, Arrays.<Object>asList(values));
    }

    /**
     * 属于（int 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isIn(int... values) {
        List<Object> boxed = new ArrayList<Object>(values.length);
        for (int value : values) {
            boxed.add(value);
        }
        return new IsIn(key, boxed);
    }

    /**
     * 属于（long 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isIn(long... values) {
        List<Object> boxed = new ArrayList<Object>(values.length);
        for (long value : values) {
            boxed.add(value);
        }
        return new IsIn(key, boxed);
    }

    /**
     * 属于（float 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isIn(float... values) {
        List<Object> boxed = new ArrayList<Object>(values.length);
        for (float value : values) {
            boxed.add(value);
        }
        return new IsIn(key, boxed);
    }

    /**
     * 属于（double 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isIn(double... values) {
        List<Object> boxed = new ArrayList<Object>(values.length);
        for (double value : values) {
            boxed.add(value);
        }
        return new IsIn(key, boxed);
    }

    /**
     * 属于（任意候选集合）。
     *
     * @param values 候选值集合
     * @return 过滤条件
     */
    public Filter isIn(Collection<?> values) {
        return new IsIn(key, values);
    }

    /**
     * 不属于（字符串候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isNotIn(String... values) {
        return new IsNotIn(key, Arrays.<Object>asList(values));
    }

    /**
     * 不属于（UUID 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isNotIn(UUID... values) {
        return new IsNotIn(key, Arrays.<Object>asList(values));
    }

    /**
     * 不属于（int 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isNotIn(int... values) {
        List<Object> boxed = new ArrayList<Object>(values.length);
        for (int value : values) {
            boxed.add(value);
        }
        return new IsNotIn(key, boxed);
    }

    /**
     * 不属于（long 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isNotIn(long... values) {
        List<Object> boxed = new ArrayList<Object>(values.length);
        for (long value : values) {
            boxed.add(value);
        }
        return new IsNotIn(key, boxed);
    }

    /**
     * 不属于（float 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isNotIn(float... values) {
        List<Object> boxed = new ArrayList<Object>(values.length);
        for (float value : values) {
            boxed.add(value);
        }
        return new IsNotIn(key, boxed);
    }

    /**
     * 不属于（double 候选）。
     *
     * @param values 候选值
     * @return 过滤条件
     */
    public Filter isNotIn(double... values) {
        List<Object> boxed = new ArrayList<Object>(values.length);
        for (double value : values) {
            boxed.add(value);
        }
        return new IsNotIn(key, boxed);
    }

    /**
     * 不属于（任意候选集合）。
     *
     * @param values 候选值集合
     * @return 过滤条件
     */
    public Filter isNotIn(Collection<?> values) {
        return new IsNotIn(key, values);
    }
}
