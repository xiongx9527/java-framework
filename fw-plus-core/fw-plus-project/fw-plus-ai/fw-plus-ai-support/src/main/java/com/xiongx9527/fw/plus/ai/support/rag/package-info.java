/**
 * 检索增强（RAG）编排契约：查询改写 / 路由 → 检索 → 聚合 → 注入。
 *
 * <p>单个检索器的能力在 {@code base.retrieval}；这里只负责把它们串成「把资料塞进提示词」的流程。</p>
 */
package com.xiongx9527.fw.plus.ai.support.rag;
