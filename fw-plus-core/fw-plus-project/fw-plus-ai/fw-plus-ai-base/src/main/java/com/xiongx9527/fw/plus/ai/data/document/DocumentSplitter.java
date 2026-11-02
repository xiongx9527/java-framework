package com.xiongx9527.fw.plus.ai.data.document;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文档切分器：把长文档切成可向量化的文本片段。
 *
 * <p><b>作用</b>：受嵌入模型上下文长度与检索粒度限制，文档必须先切分。切分策略（按段落 / 句子 /
 * 固定长度 + 重叠）由实现决定，端口只约定「文档 → 片段列表」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.DocumentSplitter}。</p>
 *
 * <p><b>实现约定</b>：切分结果保留原文档元数据；不得返回 {@code null}（无内容时返回空列表）。</p>
 *
 * @author xiongxiao
 */
public interface DocumentSplitter {

    /**
     * 切分单个文档。
     *
     * @param document 文档
     * @return 文本片段列表，永不为 {@code null}
     */
    List<TextSegment> split(Document document);

    /**
     * 切分多个文档。
     *
     * @param documents 文档列表
     * @return 汇总后的文本片段
     */
    default List<TextSegment> splitAll(List<Document> documents) {
        List<TextSegment> segments = new ArrayList<TextSegment>();
        if (documents != null) {
            for (Document document : documents) {
                segments.addAll(split(document));
            }
        }
        return segments;
    }

    /**
     * 切分多个文档（可变参数）。
     *
     * @param documents 文档
     * @return 汇总后的文本片段
     */
    default List<TextSegment> splitAll(Document... documents) {
        return splitAll(documents == null ? new ArrayList<Document>() : Arrays.asList(documents));
    }
}
