package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class FtlInfoMagicTemplateLoader implements TemplateLoader  {

    private static final String LOCALE_SUFFIX_PATTERN = "(_[a-z]{2}_[A-Z]{2})$";

    private FreemarkerMagicDynamicRegistry getFreemarkerMagicDynamicRegistry() {
        return SpringUtils.getBean(FreemarkerMagicDynamicRegistry.class);
    }

    /**
     * 获取基础模板路径，去除模板路径中的语言环境后缀
     * 
     * @param templatePath 模板路径
     * @return 去除语言环境后缀后的基础模板路径
     */
    private String getBaseTemplatePath(String templatePath) {
        if (templatePath == null || templatePath.isEmpty()) {
            return templatePath;
        }
        // 仅在末尾疑似有 _xx_XX 格式后缀时才执行正则替换
        int lastUnderscore = templatePath.lastIndexOf('_');
        if (lastUnderscore > 0 && (templatePath.length() - lastUnderscore) == 6) {
            return templatePath.replaceAll(LOCALE_SUFFIX_PATTERN, "");
        }
        return templatePath;
    }

    /**
     * 查找并获取指定路径的FreeMarker模板源
     *
     * @param s 模板路径，可以是相对路径或绝对路径
     * @return 模板源对象（FtlInfo）
     * @throws IOException 当路径非法或注册表未初始化时抛出
     */
    @Override
    public Object findTemplateSource(String s) throws IOException {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.freemarker.templatePathRequired"));
        }

        String path = s;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        path = getBaseTemplatePath(path);

        FreemarkerMagicDynamicRegistry registry = getFreemarkerMagicDynamicRegistry();
        if (registry == null) {
            throw new IllegalStateException(I18nMessageResolver.resolveKey("fw.boot.freemarker.registryNotInitialized"));
        }

        return registry.getMapping(path);
    }

    @Override
    public long getLastModified(Object o) {
        return ((FtlInfo) o).getUpdateTime();
    }

    @Override
    public Reader getReader(Object o, String s) throws IOException {
        if (!(o instanceof FtlInfo)) {
            throw new IllegalArgumentException("Invalid argument: object must be an instance of FtlInfo");
        }
        FtlInfo ftlInfo = (FtlInfo) o;
        String script = ftlInfo.getScript();
        if (script == null) {
            throw new IOException("Template script is null");
        }
        return new StringReader(script);
    }

    @Override
    public void closeTemplateSource(Object o) throws IOException {
        // 无需特殊清理
    }
}