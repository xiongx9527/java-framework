package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class FtlInfoMagicTemplateLoader implements TemplateLoader  {


    /**
     * 语言环境后缀模式常量
     *
     * 该正则表达式用于匹配语言环境标识符的后缀部分，
     * 格式为：下划线 + 2个小写字母的语言代码 + 下划线 + 2个大写字母的国家/地区代码
     * 例如：_zh_CN、_en_US 等
     */
    private final String LOCALE_SUFFIX_PATTERN = "(_[a-z]{2}_[A-Z]{2})$";


    /**
     * 获取FreemarkerMagicDynamicRegistry实例
     * 
     * @return FreemarkerMagicDynamicRegistry实例
     */
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
        return templatePath.replaceAll(LOCALE_SUFFIX_PATTERN, "");
    }


    /**
     * 查找并获取指定路径的FreeMarker模板对象。
     * 该方法负责处理模板路径的标准化，并从动态注册表中获取模板。
     *
     * @param s 模板路径字符串，可以是相对路径或绝对路径
     * @return 返回从动态注册表中获取的模板对象
     * @throws IOException 如果在查找过程中发生I/O错误或路径非法
     */
    @Override
    public Object findTemplateSource(String s) throws IOException {
        // 校验空值情况
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("模板路径不能为空");
        }

        String path = s;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // 添加日志便于调试
        // logger.debug("正在加载模板路径: {}", path);

        path = getBaseTemplatePath(path);

        FreemarkerMagicDynamicRegistry freemarkerMagicDynamicRegistry = getFreemarkerMagicDynamicRegistry();
        if (freemarkerMagicDynamicRegistry == null) {
            throw new IllegalStateException("Freemarker动态注册表未初始化");
        }

        return freemarkerMagicDynamicRegistry.getMapping(path);
    }


    /**
     * 获取模板对象的最后修改时间
     * 
     * @param o 模板信息对象，类型为FtlInfo
     * @return 返回模板的最后更新时间戳（毫秒）
     */
    @Override
    public long getLastModified(Object o) {
        return ((FtlInfo) o).getUpdateTime();
    }


    /**
     * 获取模板读取器
     * @param o 模板信息对象
     * @param s 模板名称
     * @return Reader 模板读取器
     * @throws IOException 如果读取过程中发生I/O错误
     */
    @Override
    public Reader getReader(Object o, String s) throws IOException {
        if (o == null || !(o instanceof FtlInfo)) {
            throw new IllegalArgumentException("Invalid argument: object must be an instance of FtlInfo");
        }
        if (s == null) {
            throw new IllegalArgumentException("Template name cannot be null");
        }
        FtlInfo ftlInfo = (FtlInfo) o;
        String script = ftlInfo.getScript();
        if (script == null) {
            throw new IOException("Template script is null");
        }
        return new StringReader(script);
    }

    /**
     * 关闭模板源
     * 空实现，因为模板源不需要特殊关闭处理
     *
     * @param o 模板源对象
     * @throws IOException IO异常
     */
    @Override
    public void closeTemplateSource(Object o) throws IOException {

    }

}
