package com.xiongx9527.fw.plus.ai.data.segment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文本片段转换器：对片段做清洗 / 富化（补上下文、去敏感词等）。
 *
 * <p><b>作用</b>：切分后、向量化前的一道加工；与文档转换器同构，只是作用在片段粒度。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.segment.TextSegmentTransformer}。</p>
 *
 * @author xiongxiao
 */
public interface TextSegmentTransformer {

    /**
     * 转换单个片段。
     *
     * @param textSegment 原片段
     * @return 转换后的片段
     */
    TextSegment transform(TextSegment textSegment);

    /**
     * 转换多个片段。
     *
     * @param textSegments 原片段列表
     * @return 转换后的片段列表
     */
    default List<TextSegment> transformAll(List<TextSegment> textSegments) {
        List<TextSegment> transformed = new ArrayList<TextSegment>();
        if (textSegments != null) {
            for (TextSegment textSegment : textSegments) {
                transformed.add(transform(textSegment));
            }
        }
        return transformed;
    }

    /**
     * 转换多个片段（可变参数）。
     *
     * @param textSegments 原片段
     * @return 转换后的片段列表
     */
    default List<TextSegment> transformAll(TextSegment... textSegments) {
        return transformAll(textSegments == null ? new ArrayList<TextSegment>() : Arrays.asList(textSegments));
    }
}
