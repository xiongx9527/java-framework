package com.xiongx9527.fw.plus.ai.store.embedding;

import java.util.Collections;
import java.util.List;

/**
 * 向量检索结果。
 *
 * <p><b>作用</b>：包住匹配项列表，为后续增加「耗时 / 是否被截断 / 原始得分归一化」等字段留出扩展点，
 * 避免直接把 {@code List<EmbeddingMatch>} 暴露给上层。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.EmbeddingSearchResult}。</p>
 *
 * @param <Embedded> 被嵌入对象类型
 * @author xiongxiao
 */
public class EmbeddingSearchResult<Embedded> {

    private final List<EmbeddingMatch<Embedded>> matches;

    /**
     * 构造检索结果。
     *
     * @param matches 匹配项列表，{@code null} 视为空
     */
    public EmbeddingSearchResult(List<EmbeddingMatch<Embedded>> matches) {
        this.matches = matches == null ? Collections.<EmbeddingMatch<Embedded>>emptyList() : matches;
    }

    /**
     * 匹配项列表。
     *
     * @return 按相似度降序排列的匹配项，非 {@code null}
     */
    public List<EmbeddingMatch<Embedded>> matches() {
        return matches;
    }

    @Override
    public String toString() {
        return "EmbeddingSearchResult{matches=" + matches.size() + "}";
    }
}
