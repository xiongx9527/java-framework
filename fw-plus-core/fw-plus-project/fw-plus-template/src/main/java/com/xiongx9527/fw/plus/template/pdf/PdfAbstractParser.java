package com.xiongx9527.fw.plus.template.pdf;

import cn.hutool.http.HttpUtil;
import com.xiongx9527.fw.plus.template.FileChange;
import com.xiongx9527.fw.plus.template.FileParser;
import com.xiongx9527.fw.plus.template.fop.FreemarkerDataSource;
import org.apache.commons.lang3.StringUtils;
import org.dromara.pdf.fop.handler.TemplateHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
/**
 * PdfAbstractParser抽象类实现了FileParser和FileChange接口，用于解析PDF文件和对PDF文件进行转换操作。
 *
 * <p>该类作为PDF解析器的抽象基类，定义了解析PDF文件的基本框架和通用方法。
 * 具体的解析实现需要由子类来完成。</p>
 *
 * @since 1.0
 */
public abstract class PdfAbstractParser implements FileParser , FileChange {
    /**
     * FreeMarker模板引擎配置对象
     * <p>
     * 该字段用于存储FreeMarker模板引擎的核心配置信息，
     * 包括模板加载路径、编码设置、模板缓存等配置参数。
     * 通过该配置对象可以创建和管理FreeMarker模板处理流程。
     */
    private final freemarker.template.Configuration config;


    /**
     * PDF抽象解析器默认构造函数
     * 使用默认的FreeMarker配置初始化解析器
     */
    public PdfAbstractParser() {
        this(new com.xiongx9527.fw.plus.template.freemarker.FreemarkerConfiguration());
    }

    /**
     * PDF抽象解析器构造函数
     *
     * @param config FreeMarker配置对象，用于模板处理
     */
    public PdfAbstractParser(freemarker.template.Configuration config) {
        this.config = config;
    }

    /**
     * 解析并转换数据模板
     *
     * @param data       需要解析的原始数据字节数组
     * @param map        用于数据源的键值对映射
     * @param configPath 配置文件路径，可为空
     * @return 转换后的字节数据数组
     * @throws IllegalArgumentException 当参数不合法时抛出
     * @throws IOException              当IO操作出现异常时抛出
     */
    public byte[] parse(byte[] data, Map<String, Object> map, String configPath) throws IllegalArgumentException, IOException {
        // 构建数据源
        FreemarkerDataSource dataSource = new FreemarkerDataSource(this.config, new String(data, StandardCharsets.UTF_8), map);
        // 使用 try-with-resources 确保所有资源正确释放
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(1024, data.length / 2))) {
            org.dromara.pdf.fop.core.base.Template template = TemplateHandler.Template.build();
            try {
                if (StringUtils.isNotBlank(configPath)) {
                    template.setConfigPath(configPath);
                }
                template.setDataSource(dataSource).transform(outputStream);
                // 返回转换后的字节数据而不是 null
                return outputStream.toByteArray();
            } finally {
                // 确保模板资源被正确清理
                if (template != null) {
                    // 如果Template有close或destroy方法应该调用
                    // template.close(); 或 template.destroy();
                }
            }
        }
    }

    /**
     * 解析数据并返回处理后的字节数组
     *
     * @param data 需要解析的原始字节数据
     * @param map 包含解析配置信息的映射表
     * @param configPath 配置文件路径
     * @return 处理后的字节数组结果
     * @throws Exception 解析过程中可能抛出的异常
     */
    public abstract byte[] parseByData(byte[] data, Map<String, Object> map, String configPath) ;

    /**
     * 通过URL解析数据
     *
     * @param url URL地址，用于下载需要解析的数据
     * @param map 参数映射，包含解析过程中需要使用的配置参数
     * @param configPath 配置文件路径，指定解析规则的配置文件位置
     * @return 解析后的字节数组结果
     * @throws Exception 当URL下载失败或解析过程中出现错误时抛出异常
     */
    public byte[] parseByUrl(String url, Map<String, Object> map, String configPath) throws Exception {
        return parseByData(HttpUtil.downloadBytes(url), map, configPath);
    }

}
