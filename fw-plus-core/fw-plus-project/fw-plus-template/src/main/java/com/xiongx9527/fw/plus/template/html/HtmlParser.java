package com.xiongx9527.fw.plus.template.html;

import com.xiongx9527.fw.plus.template.filechange.AbstractHtml2Pdf;
import com.xiongx9527.fw.plus.template.openpdf.OpenPdfHtml2Pdf;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
/**
 * HtmlParser类实现了HtmlAbstractParser接口，用于解析HTML文件和对HTML文件进行转换操作。
 *
 * <p>该类作为HTML解析器的具体实现，负责解析HTML文件的内容并根据参数进行替换。
 * 它利用FreeMarker模板引擎处理HTML模板，支持将HTML内容转换为PDF格式。</p>
 *
 * @since 1.0
 */
@Slf4j
public class HtmlParser extends HtmlAbstractParser {

    /**
     * HTML解析器类的配置对象，用于处理FreeMarker模板配置
     */
    private final freemarker.template.Configuration config;


    /**
     * HTML转PDF处理器实例
     *
     * 该字段用于存储HTML到PDF转换器的具体实现对象，
     * 通过依赖注入的方式提供具体的转换功能。
     */
    private final AbstractHtml2Pdf html2Pdf;

    /**
     * 构造函数，初始化HTML解析器
     * 创建一个新的FreeMarker配置实例用于模板处理
     */
    public HtmlParser() {
        this.config = new com.xiongx9527.fw.plus.template.freemarker.FreemarkerConfiguration();
        this.html2Pdf = new OpenPdfHtml2Pdf();
    }

    /**
     * 构造函数，使用指定的HTML转PDF处理器初始化HTML解析器
     * 创建一个新的FreeMarker配置实例用于模板处理，并使用传入的HTML转PDF处理器
     *
     * @param html2Pdf HTML转PDF处理器实例
     */
    public HtmlParser(AbstractHtml2Pdf html2Pdf) {
        this.config = new com.xiongx9527.fw.plus.template.freemarker.FreemarkerConfiguration();
        this.html2Pdf = html2Pdf;
    }

    /**
     * 根据输入的字节数据和参数映射解析模板内容
     *
     * @param data 包含模板内容的字节数组
     * @param map 用于模板替换的参数映射
     * @return 解析后的模板内容字节数组
     * @throws Exception 模板解析过程中可能抛出的异常
     */
    @Override
    public byte[] parseByData(byte[] data, Map<String, Object> map) throws Exception {
        StringWriter out = new StringWriter();
        try {
            // 将字节数组转换为字符串并创建模板对象
            String templateContent = new String(data, StandardCharsets.UTF_8);
            Template template = new Template(templateContent, templateContent, config);

            // 处理模板并生成最终内容
            template.process(map, out);
            return out.toString().getBytes(StandardCharsets.UTF_8);
        } finally {
            // 确保StringWriter被正确关闭
            try {
                out.close();
            } catch (Exception e) {
                log.warn("Failed to close StringWriter", e);
            }
        }
    }

    /**
     * 将HTML字节数组转换为HTML字节数组
     *
     * @param srcBytes 源HTML字节数据
     * @return 转换后的HTML字节数据
     * @throws UnsupportedOperationException HTML到HTML的转换不被支持
     */
    @Override
    public byte[] toHmtl(byte[] srcBytes) {
        throw new UnsupportedOperationException("Html to Html conversion is not supported");
    }

    /**
     * 将源字节数组转换为PDF格式
     *
     * @param srcBytes 源字节数组
     * @return 转换后的PDF字节数组
     * @throws RuntimeException 当转换过程中发生异常时抛出
     */
    @Override
    public byte[] toPdf(byte[] srcBytes) {
        try {
            // 调用html2Pdf工具进行转换
            return html2Pdf.change(srcBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
