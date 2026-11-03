/**
 * 检索基础能力：一个通用检索契约 + 可组合的装饰器。
 *
 * <p>核心只有两件事：{@link com.xiongx9527.fw.plus.ai.support.retrieval.Retriever} 把「问题」
 * 变成「带分数的条目」，{@link com.xiongx9527.fw.plus.ai.support.retrieval.Retrieved} 承载任意条目类型。
 * 条目可以是文档、表结构、术语、SQL 示例、图节点——检索不假设业务对象。</p>
 *
 * <p>融合、重排、阈值、截断、监听都是同一契约上的装饰器，按需叠加：
 * {@code Retriever → Reranking → Threshold → TopN → Listening}。</p>
 */
package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;