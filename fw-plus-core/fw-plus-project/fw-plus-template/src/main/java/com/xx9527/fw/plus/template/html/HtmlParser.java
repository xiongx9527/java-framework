package com.xx9527.fw.plus.template.html;

import com.xx9527.fw.plus.common.util.SpringUtils;
import com.xx9527.fw.plus.template.filechange.AbstractHtml2Pdf;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
/**
 * HtmlParser类实现了HtmlAbstractParser接口，用于解析HTML文件和对HTML文件进行转换操作。
 *
 * <p>该类作为HTML解析器的具体实现，负责解析HTML文件的内容并根据参数进行替换。
 * 它利用FreeMarker模板引擎处理HTML模板。</p>
 *
 * @since 1.0
 */
@Slf4j
public class HtmlParser extends HtmlAbstractParser {

    /**
     * HTML解析器类的配置对象，用于处理FreeMarker模板配置
     */
    private final freemarker.template.Configuration config;


    private final AbstractHtml2Pdf html2Pdf;

    /**
     * 构造函数，初始化HTML解析器
     * 创建一个新的FreeMarker配置实例用于模板处理
     */
    public HtmlParser() {
        this.config = new com.xx9527.fw.plus.template.freemarker.FreemarkerConfiguration();
        this.html2Pdf = null;
    }

    /**
     * 构造函数，使用指定的HTML转PDF处理器初始化HTML解析器
     *
     * @param html2Pdf HTML转PDF处理器实例
     */
    public HtmlParser(AbstractHtml2Pdf html2Pdf) {
        this.config = new com.xx9527.fw.plus.template.freemarker.FreemarkerConfiguration();
        this.html2Pdf = html2Pdf;
    }

    /**
     * 根据提供的映射关系解析HTML文档数据
     *
     * <p>该方法支持两种解析方式：
     * 1. 如果提供了beanName参数，则从Spring容器中获取对应的HtmlAbstractParser实现类实例进行解析
     * 2. 如果未提供beanName参数，则使用当前实例进行解析</p>
     *
     * @param beanName Spring容器中HtmlAbstractParser实现类的Bean名称，可为null或空字符串
     * @param data 原始HTML文档的字节数组数据
     * @param map 包含模板变量名和对应替换值的映射关系，键为变量名，值为替换内容
     * @return 解析并替换后的HTML文档字节数组
     * @throws Exception 当文档解析过程中发生错误时抛出异常
     */
    @Override
    public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) throws Exception {
        if (StringUtils.isNotBlank(beanName)) {
            return SpringUtils.getBean(beanName, HtmlAbstractParser.class).parseByData(data, map);
        }
        return this.parseByData(data, map);
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
    public byte[] toHtml(byte[] srcBytes) {
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
        if (html2Pdf == null) {
            throw new UnsupportedOperationException("Html to Pdf conversion is not supported");
        }
        try {
            return html2Pdf.change(srcBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
