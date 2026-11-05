/**
 * 检索基础能力：通用检索契约 + 可直接运行的单路实现。
 *
 * <ul>
 *   <li>契约：{@link com.xiongx9527.fw.plus.ai.base.retrieval.Retriever}（问题 → 带分数的条目）、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved}（任意条目类型）、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery}、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalResult}、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.Reranker}、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalAugmentor}、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalCapability}</li>
 *   <li>实现：{@link com.xiongx9527.fw.plus.ai.base.retrieval.VectorRetriever}（向量检索）、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.FullTextRetriever}（TF-IDF 词法检索）、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.TextTokenizer}、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.DefaultReranker}（{@code RerankPort} → {@code Reranker}）、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.DefaultRetrievalAugmentor}、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.ChainedRetrievalAugmentor}、
 *       {@link com.xiongx9527.fw.plus.ai.base.retrieval.DefaultRetrievalCapability}</li>
 * </ul>
 *
 * <p>融合、重排策略、阈值、截断、多路管线等编排在 {@code fw-plus-ai-support}；
 * 词法 / 关键词检索器（{@code KeywordRetriever} 等）也在 support，不在 base。</p>
 */
package com.xiongx9527.fw.plus.ai.base.retrieval;
