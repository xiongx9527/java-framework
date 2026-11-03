package com.xiongx9527.fw.plus.ai.base.retrieval;

import java.util.List;

/**
 * 通用检索契约：问题 → 带分数的条目（按相关度降序）。
 *
 * <p>不假设条目类型，因此向量检索、全文检索、图检索、业务规则检索都实现同一个接口，
 * 融合 / 重排 / 阈值 / 截断等装饰器对它们一视同仁。</p>
 *
 * @param <T> 条目类型
 */
public interface Retriever<T> {

    List<Retrieved<T>> retrieve(RetrievalQuery query);
}
