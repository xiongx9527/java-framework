package com.xiongx9527.fw.plus.ai.data.document;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;

/**
 * 文档：一段完整文本 + 元数据。
 *
 * <p><b>作用</b>：文档装载（loader/parser）的产物，也是切分（splitter）、转换（transformer）的输入；
 * 切分后得到 {@link TextSegment} 供向量化与检索。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.Document}
 * （接口 + 三个元数据键常量 + {@code from}/{@code document} 静态工厂 + {@code toTextSegment()}）。
 * Spring AI 等价 {@code org.springframework.ai.document.Document}。</p>
 *
 * <p><b>实现约定</b>：实现类不可变；{@link #metadata()} 不得返回 {@code null}。</p>
 *
 * @author xiongxiao
 */
public interface Document {

    /** 元数据键：文件名。 */
    String FILE_NAME = "file_name";

    /** 元数据键：文件所在绝对目录。 */
    String ABSOLUTE_DIRECTORY_PATH = "absolute_directory_path";

    /** 元数据键：文档来源 URL。 */
    String URL = "url";

    /**
     * 文档全文。
     *
     * @return 文本
     */
    String text();

    /**
     * 文档元数据。
     *
     * @return 元数据，永不为 {@code null}
     */
    Metadata metadata();

    /**
     * 转为文本片段（默认携带同一份元数据）。
     *
     * @return 文本片段
     */
    default TextSegment toTextSegment() {
        return TextSegment.from(text(), metadata());
    }

    /**
     * 由文本创建文档（空元数据）。
     *
     * @param text 文本
     * @return 文档
     */
    static Document from(String text) {
        return new DefaultDocument(text);
    }

    /**
     * 由文本与元数据创建文档。
     *
     * @param text     文本
     * @param metadata 元数据
     * @return 文档
     */
    static Document from(String text, Metadata metadata) {
        return new DefaultDocument(text, metadata);
    }

    /**
     * 由文本创建文档（{@link #from(String)} 的别名）。
     *
     * @param text 文本
     * @return 文档
     */
    static Document document(String text) {
        return from(text);
    }

    /**
     * 由文本与元数据创建文档（{@link #from(String, Metadata)} 的别名）。
     *
     * @param text     文本
     * @param metadata 元数据
     * @return 文档
     */
    static Document document(String text, Metadata metadata) {
        return from(text, metadata);
    }
}
