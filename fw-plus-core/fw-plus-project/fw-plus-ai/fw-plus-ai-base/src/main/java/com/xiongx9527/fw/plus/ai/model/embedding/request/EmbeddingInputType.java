package com.xiongx9527.fw.plus.ai.model.embedding.request;

/**
 * 嵌入输入用途。
 *
 * <p><b>作用</b>：部分模型（如 bge-m3、voyage、Cohere）对「查询」与「文档」使用不同的编码前缀，
 * 区分用途才能让查询向量与文档向量落在同一可比空间。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.request.EmbeddingInputType}。</p>
 *
 * @author xiongxiao
 */
public enum EmbeddingInputType {

    /** 查询侧输入（检索时用）。 */
    QUERY,

    /** 文档侧输入（入库时用）。 */
    DOCUMENT
}
