package com.xiongx9527.fw.plus.ai.rag.content;

/**
 * 内容元数据键：描述「这条内容是怎么被选中的」。
 *
 * <p><b>作用</b>：检索得分、重排得分、向量记录 ID 都挂在内容上，供聚合器排序与调用方溯源。
 * 用枚举而非字符串键，避免拼写漂移。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.content.ContentMetadata}。</p>
 *
 * @author xiongxiao
 */
public enum ContentMetadata {

    /** 向量检索相似度得分。 */
    SCORE,

    /** 重排（rerank）得分。 */
    RERANKED_SCORE,

    /** 向量库中的记录 ID。 */
    EMBEDDING_ID
}
