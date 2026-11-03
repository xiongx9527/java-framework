/**
 * 检索基础能力：通用检索契约 + 单路检索实现。
 *
 * <ul>
 *   <li>契约：{@link com.xiongx9527.fw.plus.ai.base.retrieval.Retriever}（问题 → 带分数的条目）、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved}（任意条目类型）、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery}、{@link com.xiongx9527.fw.plus.ai.base.retrieval.Reranker}</li>
 *   <li>实现：{@link com.xiongx9527.fw.plus.ai.base.retrieval.VectorRetriever}（向量检索）、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.KeywordRetriever}（全文 / 词法检索，可注入
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.LexicalScorer} 换打分口径）</li>
 * </ul>
 *
 * <p>融合、重排、阈值、截断、索引模式（高质量 / 经济）等编排在 {@code fw-plus-ai-support}，
 * 它们只依赖这里的契约与单路实现。</p>
 */
package com.xiongx9527.fw.plus.ai.base.retrieval;
