package com.xiongx9527.fw.plus.ai.data.pdf;

import java.net.URI;

/**
 * PDF 文件：URL 或 base64 承载的 PDF 数据。
 *
 * <p><b>作用</b>：多模态消息（{@code PdfFileContent}）的 PDF 载体；支持直接交给支持文档输入的模型。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.pdf.PdfFile}
 * （{@code DEFAULT_MIME_TYPE = "application/pdf"}）。</p>
 *
 * @author xiongxiao
 */
public class PdfFile {

    /** 默认 MIME 类型。 */
    public static final String DEFAULT_MIME_TYPE = "application/pdf";

    private final URI url;
    private final String base64Data;
    private final String mimeType;

    private PdfFile(Builder builder) {
        this.url = builder.url;
        this.base64Data = builder.base64Data;
        this.mimeType = builder.mimeType;
    }

    /**
     * PDF 地址。
     *
     * @return URI，可能为 {@code null}
     */
    public URI url() { return url; }

    /**
     * base64 编码的 PDF 数据。
     *
     * @return base64 字符串，可能为 {@code null}
     */
    public String base64Data() { return base64Data; }

    /**
     * MIME 类型。
     *
     * @return MIME 类型，默认 {@link #DEFAULT_MIME_TYPE}
     */
    public String mimeType() { return mimeType; }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() { return new Builder(); }

    @Override
    public String toString() {
        return "PdfFile{url=" + url + ", mimeType='" + mimeType + "'}";
    }

    /** {@link PdfFile} 构建器。 */
    public static final class Builder {
        private URI url;
        private String base64Data;
        private String mimeType = DEFAULT_MIME_TYPE;

        public Builder url(URI url) { this.url = url; return this; }
        public Builder base64Data(String base64Data) { this.base64Data = base64Data; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public PdfFile build() { return new PdfFile(this); }
    }
}
