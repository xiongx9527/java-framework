package com.xiongx9527.fw.plus.template.openpdf;

import com.xiongx9527.fw.plus.template.filechange.AbstractHtml2Pdf;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
/**
 * OpenPdfHtml2Pdf类实现了AbstractHtml2Pdf接口，用于将HTML文件转换为PDF格式的功能。
 *
 * <p>该类作为HTML转PDF功能的具体实现，负责解析HTML文件的内容并将其转换为PDF文档。
 * 它利用OpenPDF库处理PDF文档的生成，支持将HTML内容转换为PDF格式。</p>
 *
 * @since 1.0
 */
public class OpenPdfHtml2Pdf extends AbstractHtml2Pdf {
    /**
     * 将HTML格式的源数据转换为PDF格式的字节数组
     *
     * @param srcData HTML格式的源数据字节数组，不能为null
     * @return 转换后的PDF格式字节数组
     * @throws Exception 转换过程中可能抛出的异常
     */
    @Override
    public byte[] change(byte[] srcData) throws Exception {
        if (srcData == null) {
            throw new IllegalArgumentException("srcData cannot be null");
        }

        ITextRenderer renderer = new ITextRenderer();
        // 设置文档内容
        renderer.setDocumentFromString(new String(srcData, StandardCharsets.UTF_8));
        // 进行布局
        renderer.layout();
        // 创建PDF
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            renderer.createPDF(os);
            return os.toByteArray();
        }
    }
}
