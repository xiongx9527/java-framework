package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.List;

/**
 * 关键字打分：给定从问题里抽取的关键词，判断它们在文档（业务对象）上的命中程度。
 *
 * <p>与全文检索的区别：全文看整段文本的词频相似度，关键字看「抽取出的词是否命中
 * 结构化字段」（表名 / 注释 / 字段名等），命中口径由实现决定。</p>
 */
public interface KeywordScorer {

    /** 返回 0~1 的命中分；0 表示完全没命中。 */
    double score(List<String> keywords, Document document);
}
