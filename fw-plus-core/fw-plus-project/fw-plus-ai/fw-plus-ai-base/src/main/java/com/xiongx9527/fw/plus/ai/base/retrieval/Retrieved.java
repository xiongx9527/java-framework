package com.xiongx9527.fw.plus.ai.base.retrieval;

import java.util.function.Function;

/**
 * 一条检索结果：任意条目 + 相关度分数（0~1）+ 来源。
 *
 * <p>用泛型承载条目，是为了让同一套检索链路既能返回文档，也能返回表结构、术语、
 * 图节点等业务对象——上层不需要为每种对象各写一套召回 / 融合 / 重排逻辑。</p>
 */
public final class Retrieved<T> {

    private final T item;

    private final double score;

    private final String source;

    private Retrieved(T item, double score, String source) {
        this.item = item;
        this.score = score;
        this.source = source;
    }

    public static <T> Retrieved<T> of(T item, double score) {
        return new Retrieved<T>(item, score, null);
    }

    public static <T> Retrieved<T> of(T item, double score, String source) {
        return new Retrieved<T>(item, score, source);
    }

    public T getItem() {
        return item;
    }

    public double getScore() {
        return score;
    }

    /** 来源（检索器 / 集合名），用于多路融合时追溯；可为 {@code null}。 */
    public String getSource() {
        return source;
    }

    /** 保留分数与来源，把条目换成另一种类型。 */
    public <R> Retrieved<R> map(Function<T, R> mapper) {
        return new Retrieved<R>(mapper == null ? null : mapper.apply(item), score, source);
    }

    /** 替换分数（重排 / 融合时使用）。 */
    public Retrieved<T> withScore(double value) {
        return new Retrieved<T>(item, value, source);
    }

    @Override
    public String toString() {
        return "Retrieved{score=" + score + ", source=" + source + ", item=" + item + '}';
    }
}
