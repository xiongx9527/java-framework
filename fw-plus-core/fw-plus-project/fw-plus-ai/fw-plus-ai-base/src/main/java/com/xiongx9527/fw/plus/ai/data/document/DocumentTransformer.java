package com.xiongx9527.fw.plus.ai.data.document;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档转换器：对文档做清洗 / 富化（去噪、补元数据、脱敏等）。
 *
 * <p><b>作用</b>：装载之后、切分之前的一道加工，避免把清洗逻辑写进解析器。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.DocumentTransformer}。</p>
 *
 * @author xiongxiao
 */
public interface DocumentTransformer {

    /**
     * 转换单个文档。
     *
     * @param document 原文档
     * @return 转换后的文档
     */
    Document transform(Document document);

    /**
     * 转换多个文档。
     *
     * @param documents 原文档列表
     * @return 转换后的文档列表
     */
    default List<Document> transformAll(List<Document> documents) {
        List<Document> transformed = new ArrayList<Document>();
        if (documents != null) {
            for (Document document : documents) {
                transformed.add(transform(document));
            }
        }
        return transformed;
    }
}
