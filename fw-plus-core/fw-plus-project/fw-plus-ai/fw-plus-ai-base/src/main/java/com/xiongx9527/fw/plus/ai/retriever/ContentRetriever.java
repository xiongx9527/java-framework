package com.xiongx9527.fw.plus.ai.retriever;

import com.xiongx9527.fw.plus.ai.rag.content.Content;

import java.util.List;

/**
 * 内容检索端口：把一个查询变成一组相关内容。
 *
 * <p><b>作用</b>：RAG 的入口。实现可以是向量检索、关键词检索、混合检索或 Web 搜索，
 * 调用方只面对「查询 → 内容列表」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.content.retriever.ContentRetriever}；
 * Spring AI 等价 {@code VectorStore#similaritySearch(SearchRequest)}。</p>
 *
 * <p><b>实现约定</b>：无结果时返回空列表，不返回 {@code null}；不得修改传入的查询对象。</p>
 *
 * @author xiongxiao
 */
public interface ContentRetriever {

    /**
     * 检索内容。
     *
     * @param query 查询
     * @return 内容列表，永不为 {@code null}
     */
    List<Content> retrieve(Query query);
}
