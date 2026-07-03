package com.xiongx9527.fw.plus.template.fop;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Freemarker数据源实现类
 * 继承自FreemarkerAbstractDataSource，用于处理Freemarker模板的数据源操作
 */
public class FreemarkerDataSource extends FreemarkerAbstractDataSource {

    /**
     * 配置对象实例
     * 用于存储和管理应用程序的配置信息
     */
    private Configuration config = null;

    /**
     * 构造函数
     *
     * @param config       Freemarker配置对象
     * @param templateName 模板名称
     * @param templateData 模板数据
     */
    public FreemarkerDataSource(Configuration config, String templateName, Map<String, Object> templateData) {
        super();
        this.config = config;
        this.templatePath = templateName;
        this.templateData = templateData;
    }

    /**
     * 加载模板输入流
     *
     * @return 返回模板输入流
     */
    @Override
    protected InputStream loadTemplateInputStream() {
        return new ByteArrayInputStream(this.templatePath.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 处理模板
     *
     * @return 返回处理后的模板输入流
     * @throws IOException      IO异常
     * @throws TemplateException 模板异常
     */
    protected InputStream processTemplate() throws IOException, TemplateException {
        // 创建输出流和写入器来处理模板
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             Writer writer = new BufferedWriter(new OutputStreamWriter(output))) {
            // 处理模板
            this.config.getTemplate(this.templatePath).process(this.templateData, writer);
            // 返回输入流
            return new BufferedInputStream(new ByteArrayInputStream(output.toByteArray()));
        }
    }

}


