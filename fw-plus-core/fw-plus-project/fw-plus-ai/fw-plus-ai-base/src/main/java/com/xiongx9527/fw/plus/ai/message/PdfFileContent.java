package com.xiongx9527.fw.plus.ai.message;

import com.xiongx9527.fw.plus.ai.data.pdf.PdfFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * PDF 消息内容（多模态输入）。
 *
 * <p><b>作用</b>：让支持文档输入的模型直接读取 PDF（保留版式与表格），避免先转文本再喂。
 * 与「文档解析（Tika / PDFBox）→ 文本」的路径互补：前者交给模型，后者交给 RAG。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.message.PdfFileContent}。</p>
 *
 * @author xiongxiao
 */
public class PdfFileContent implements Content {

    private final PdfFile pdfFile;

    /**
     * 由 PDF 地址创建。
     *
     * @param url PDF 地址
     */
    public PdfFileContent(URI url) {
        this(PdfFile.builder().url(url).build());
    }

    /**
     * 由 PDF 地址字符串创建。
     *
     * @param url PDF 地址
     */
    public PdfFileContent(String url) {
        this(URI.create(url));
    }

    /**
     * 由 base64 数据与 MIME 类型创建。
     *
     * @param base64Data base64 编码的 PDF
     * @param mimeType   MIME 类型
     */
    public PdfFileContent(String base64Data, String mimeType) {
        this(PdfFile.builder().base64Data(base64Data).mimeType(mimeType).build());
    }

    /**
     * 由 {@link PdfFile} 创建。
     *
     * @param pdfFile PDF 文件
     */
    public PdfFileContent(PdfFile pdfFile) {
        if (pdfFile == null) {
            throw new IllegalArgumentException("pdfFile must not be null");
        }
        this.pdfFile = pdfFile;
    }

    @Override
    public ContentType type() {
        return ContentType.PDF;
    }

    /**
     * PDF 文件。
     *
     * @return PDF 文件对象，永不为 {@code null}
     */
    public PdfFile pdfFile() {
        return pdfFile;
    }

    @Override
    public String toString() {
        return "PdfFileContent{pdfFile=" + pdfFile + '}';
    }

    /**
     * 由 PDF 地址创建。
     *
     * @param url PDF 地址
     * @return PDF 内容
     */
    public static PdfFileContent from(URI url) {
        return new PdfFileContent(url);
    }

    /**
     * 由 PDF 地址字符串创建。
     *
     * @param url PDF 地址
     * @return PDF 内容
     */
    public static PdfFileContent from(String url) {
        return new PdfFileContent(url);
    }

    /**
     * 由 base64 数据与 MIME 类型创建。
     *
     * @param base64Data base64 编码的 PDF
     * @param mimeType   MIME 类型
     * @return PDF 内容
     */
    public static PdfFileContent from(String base64Data, String mimeType) {
        return new PdfFileContent(base64Data, mimeType);
    }

    /**
     * 由本地文件创建（读取为 base64，MIME 使用默认值）。
     *
     * @param path 文件路径
     * @return PDF 内容
     */
    public static PdfFileContent from(Path path) {
        return from(path, PdfFile.DEFAULT_MIME_TYPE);
    }

    /**
     * 由本地文件创建（读取为 base64）。
     *
     * @param path     文件路径
     * @param mimeType MIME 类型
     * @return PDF 内容
     */
    public static PdfFileContent from(Path path, String mimeType) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new PdfFileContent(Base64.getEncoder().encodeToString(bytes), mimeType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read PDF from " + path, e);
        }
    }

    /**
     * 由 {@link PdfFile} 创建。
     *
     * @param pdfFile PDF 文件
     * @return PDF 内容
     */
    public static PdfFileContent from(PdfFile pdfFile) {
        return new PdfFileContent(pdfFile);
    }
}
