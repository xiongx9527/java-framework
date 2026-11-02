package com.xiongx9527.fw.plus.ai.data.document;

import java.io.InputStream;

/**
 * 文档解析器：把字节流解析成 {@link Document}。
 *
 * <p><b>作用</b>：TXT / PDF / Office / HTML 等格式差异都收敛在这里，装载链路只面对「流 → 文档」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.DocumentParser}；
 * 各格式实现见 {@code fw-plus-ai-native-document-parser-*} 等组件子模块。</p>
 *
 * <p><b>实现约定</b>：不得关闭传入的流（由调用方负责）；解析结果为空文本时应抛
 * {@link BlankDocumentException}。</p>
 *
 * @author xiongxiao
 */
public interface DocumentParser {

    /**
     * 解析文档。
     *
     * @param inputStream 内容流，由调用方关闭
     * @return 文档，永不为 {@code null}
     */
    Document parse(InputStream inputStream);
}
