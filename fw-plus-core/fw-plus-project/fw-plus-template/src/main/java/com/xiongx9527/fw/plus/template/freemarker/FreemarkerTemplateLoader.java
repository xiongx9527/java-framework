package com.xiongx9527.fw.plus.template.freemarker;

import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
/**
 * FreemarkerTemplateLoader类实现了TemplateLoader接口，用于加载FreeMarker模板。
 *
 * <p>该类负责根据模板名称查找模板资源，并提供模板内容的读取功能。
 * 它实现了FreeMarker模板加载器的基本功能，用于在FreeMarker中加载和使用模板。</p>
 *
 * @since 1.0
 */
public class FreemarkerTemplateLoader implements TemplateLoader {
    /**
     * 查找模板资源
     * @param name 模板名称
     * @return 模板资源对象
     * @throws IOException IO异常
     */
    @Override
    public Object findTemplateSource(String name) throws IOException {
        return name;
    }

    /**
     * 获取模板最后修改时间
     * @param templateSource 模板资源对象
     * @return 最后修改时间戳，返回-1表示不支持修改时间检查
     */
    @Override
    public long getLastModified(Object templateSource) {
        return -1;
    }

    /**
     * 获取模板读取器
     * @param templateSource 模板资源对象
     * @param encoding 编码格式
     * @return 模板内容读取器
     * @throws IOException IO异常
     */
    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        String template = (String) templateSource;
        return new StringReader(template);
    }

    /**
     * 关闭模板资源
     * @param templateSource 模板资源对象
     * @throws IOException IO异常
     */
    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {

    }
}
