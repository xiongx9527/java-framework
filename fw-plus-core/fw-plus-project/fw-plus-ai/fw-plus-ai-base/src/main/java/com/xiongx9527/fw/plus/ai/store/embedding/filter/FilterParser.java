package com.xiongx9527.fw.plus.ai.store.embedding.filter;

/**
 * 过滤条件解析器 SPI。
 *
 * <p><b>作用</b>：允许调用方以字符串形式（如 JSON / 自定义 DSL）描述过滤条件，
 * 由具体向量库或上层自行提供解析实现，避免把某一种 DSL 固化进核心接口。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.FilterParser}。</p>
 *
 * @author xiongxiao
 */
public interface FilterParser {

    /**
     * 把字符串解析为过滤条件。
     *
     * @param filter 过滤条件的字符串表示
     * @return 解析后的 {@link Filter}
     * @throws IllegalArgumentException 语法非法时抛出
     */
    Filter parse(String filter);
}
