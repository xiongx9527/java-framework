package com.xiongx9527.fw.plus.ai.retriever;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.rag.content.Content;

import java.util.ArrayList;
import java.util.List;

/**
 * 旧式检索器：只返回文本结果。
 *
 * <p><b>作用</b>：保留给既有实现；新代码应直接实现 {@link ContentRetriever}。
 * {@link #toContentRetriever()} 提供向新接口的桥接。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.retriever.Retriever}
 * （已废弃，官方同样提供 {@code toContentRetriever()} 桥接）。</p>
 *
 * @param <T> 结果类型
 * @author xiongxiao
 */
@Deprecated
public interface Retriever<T> {

    /**
     * 检索。
     *
     * @param query 查询文本
     * @return 结果列表
     */
    List<T> retrieve(String query);

    /**
     * 桥接为 {@link ContentRetriever}；仅 {@link TextSegment} 结果会被保留。
     *
     * @return 内容检索器
     */
    default ContentRetriever toContentRetriever() {
        final Retriever<T> self = this;
        return query -> {
            List<T> results = self.retrieve(query.text());
            List<Content> contents = new ArrayList<Content>();
            if (results != null) {
                for (T result : results) {
                    if (result instanceof TextSegment) {
                        contents.add(Content.from((TextSegment) result));
                    }
                }
            }
            return contents;
        };
    }
}
