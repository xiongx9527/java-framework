package com.xiongx9527.fw.plus.ai.data.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * 文档装载器：把 {@link DocumentSource} + {@link DocumentParser} 组装成 {@link Document}。
 *
 * <p><b>作用</b>：装载是「打开流 → 解析 → 合并来源元数据」的固定流程，抽成工具方法避免各组件重复实现。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.DocumentLoader}。</p>
 *
 * <p><b>实现约定</b>：解析结果的元数据优先于来源元数据（解析器更了解内容本身）。</p>
 *
 * @author xiongxiao
 */
public final class DocumentLoader {

    private DocumentLoader() {
    }

    /**
     * 装载文档。
     *
     * @param source 文档来源
     * @param parser 解析器
     * @return 文档，永不为 {@code null}
     */
    public static Document load(DocumentSource source, DocumentParser parser) {
        try (InputStream inputStream = source.inputStream()) {
            Document parsed = parser.parse(inputStream);
            Metadata metadata = new Metadata();
            if (source.metadata() != null) {
                metadata.putAll(source.metadata().toMap());
            }
            if (parsed.metadata() != null) {
                metadata.putAll(parsed.metadata().toMap());
            }
            return new DefaultDocument(parsed.text(), metadata);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load document", e);
        }
    }
}
