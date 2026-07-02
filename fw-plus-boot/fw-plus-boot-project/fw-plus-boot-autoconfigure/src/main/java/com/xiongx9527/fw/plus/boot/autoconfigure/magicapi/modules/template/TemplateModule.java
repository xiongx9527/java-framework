package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.template;

import com.xiongx9527.fw.plus.template.TemplateParser;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.util.Map;

@Slf4j
@MagicModule("template")
public class TemplateModule {
    /**
     * 模板模块类
     *
     * <p>该类负责模板相关的功能处理，通过内部的模板解析器来完成具体的模板操作。
     */
    private final TemplateParser templateParser ;

    /**
     * 构造函数
     *
     * <p>初始化模板模块，创建并配置模板解析器实例。
     */
    public TemplateModule() {
        this.templateParser = new TemplateParser();
    }

    /**
     * 构造函数，用于创建TemplateModule实例
     *
     * @param templateParser 模板解析器对象，用于处理模板解析逻辑
     */
    public TemplateModule(TemplateParser templateParser) {
        this.templateParser = templateParser;
    }

    /**
     * 根据数据字节数组和参数映射解析Word文档
     * @param data Word文档的字节数组
     * @param map 参数映射，用于替换模板中的占位符
     * @return 解析后的Word文档字节数组
     */
    public byte[] parseWordByData(byte[] data, Map<String, Object> map) {
        return this.templateParser.parseWordByData(data, map);
    }

    /**
     * 根据URL和参数映射解析Word文档
     * @param url Word文档的URL地址
     * @param map 参数映射，用于替换模板中的占位符
     * @return 解析后的Word文档字节数组
     */
    public byte[] parseWordByUrl(String url, Map<String, Object> map)  {
        return this.templateParser.parsWordByUrl(url, map);
    }

    /**
     * 根据数据字节数组和参数映射解析Excel文档
     * @param data Excel文档的字节数组
     * @param map 参数映射，用于替换模板中的占位符
     * @return 解析后的Excel文档字节数组
     */
    public byte[] parseExcelByData(byte[] data, Map<String, Object> map)  {
        return this.templateParser.parseExcelByData(data, map);
    }

    /**
     * 根据URL和参数映射解析Excel文档
     * @param url Excel文档的URL地址
     * @param map 参数映射，用于替换模板中的占位符
     * @return 解析后的Excel文档字节数组
     * @throws Exception 解析过程中可能抛出的异常
     */
    public byte[] parseExcelByUrl(String url, Map<String, Object> map) throws Exception {
        return this.templateParser.parseExcelByUrl(url, map);
    }

    /**
     * 根据数据字节数组和参数映射解析HTML文档
     * @param data HTML文档的字节数组
     * @param map 参数映射，用于替换模板中的占位符
     * @return 解析后的HTML文档字节数组
     */
    public byte[] parseHtmlByData(byte[] data, Map<String, Object> map) {
        return this.templateParser.parseHtmlByData(data, map);
    }

    /**
     * 根据URL和参数映射解析HTML文档
     * @param url HTML文档的URL地址
     * @param map 参数映射，用于替换模板中的占位符
     * @return 解析后的HTML文档字节数组
     */
    public byte[] parseHtmlByUrl(String url, Map<String, Object> map)  {
        return this.templateParser.parseHtmlByUrl(url, map);
    }

    /**
     * 根据数据字节数组、参数映射和配置路径解析PDF文档
     * @param data PDF文档的字节数组
     * @param map 参数映射，用于替换模板中的占位符
     * @param configPath 配置文件路径
     * @return 解析后的PDF文档字节数组
     */
    public byte[] parsePdfByData(byte[] data, Map<String, Object> map, String configPath)  {
        return this.templateParser.parsePdfByData(data, map, configPath);
    }

    /**
     * 根据数据字节数组和参数映射解析PDF文档
     * @param data PDF文档的字节数组
     * @param map 参数映射，用于替换模板中的占位符
     * @return 解析后的PDF文档字节数组
     */
    public byte[] parsePdfByData(byte[] data, Map<String, Object> map)  {
        return this.templateParser.parsePdfByData(data, map);
    }

    /**
     * 根据URL、参数映射和配置路径解析PDF文档
     * @param url PDF文档的URL地址
     * @param map 参数映射，用于替换模板中的占位符
     * @param configPath 配置文件路径
     * @return 解析后的PDF文档字节数组
     */
    public byte[] parsePdfByUrl(String url, Map<String, Object> map, String configPath) {
        return this.templateParser.parsePdfByUrl(url, map, configPath);
    }

    /**
     * 根据URL和参数映射解析PDF文档
     * @param url PDF文档的URL地址
     * @param map 参数映射，用于替换模板中的占位符
     * @return 解析后的PDF文档字节数组
     */
    public byte[] parsePdfByUrl(String url, Map<String, Object> map) {
        return this.templateParser.parsePdfByUrl(url, map);
    }


}
