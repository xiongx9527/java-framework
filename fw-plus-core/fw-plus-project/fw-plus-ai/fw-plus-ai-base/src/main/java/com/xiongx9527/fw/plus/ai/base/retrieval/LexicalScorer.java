package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.Document;

/**
 * 词法打分端口：给定查询文本与文档，返回 0~1 的相关度。
 *
 * <p>之所以做成端口而不是只提供一种实现：不同领域的「词法相关」口径差别很大——
 * 通用全文用 TF-IDF 余弦，而表结构召回更看重「表名完整命中 &gt; 注释命中 &gt; 子串命中」
 * 这类规则加权。领域包注入自己的打分器即可复用同一套检索链路。</p>
 */
public interface LexicalScorer {

    double score(String query, Document document);
}
