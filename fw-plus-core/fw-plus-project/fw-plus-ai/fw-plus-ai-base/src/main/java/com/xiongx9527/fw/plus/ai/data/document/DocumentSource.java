package com.xiongx9527.fw.plus.ai.data.document;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档来源：可打开的内容流 + 来源元数据。
 *
 * <p><b>作用</b>：文件系统、classpath、HTTP、对象存储的差异收敛在这里；配合 {@link DocumentLoader}
 * 把「来源 + 解析器」组装成文档。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.DocumentSource}。</p>
 *
 * <p><b>实现约定</b>：{@link #inputStream()} 每次调用都应返回新的可读流；元数据至少应包含
 * {@link Document#FILE_NAME} 或 {@link Document#URL} 之一。</p>
 *
 * @author xiongxiao
 */
public interface DocumentSource {

    /**
     * 打开内容流。
     *
     * @return 内容流，调用方负责关闭
     * @throws IOException 打开失败
     */
    InputStream inputStream() throws IOException;

    /**
     * 来源元数据。
     *
     * @return 元数据，永不为 {@code null}
     */
    Metadata metadata();
}
