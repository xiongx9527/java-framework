package com.xx9527.fw.plus.template;

import com.xx9527.fw.plus.template.excel.ExcelAbstractParser;
import com.xx9527.fw.plus.template.excel.ExcelParser;
import com.xx9527.fw.plus.template.html.HtmlAbstractParser;
import com.xx9527.fw.plus.template.html.HtmlParser;
import com.xx9527.fw.plus.template.word.WordAbstractParser;
import com.xx9527.fw.plus.template.word.WordParser;

import java.util.Map;
/**
 * TemplateParser类负责管理不同文件格式的解析器实例。
 *
 * <p>该类提供了对Word、Excel和HTML文件格式的解析操作，
 * 并根据需要初始化默认的解析器实例。</p>
 *
 * @since 1.0
 */
public class TemplateParser {

    /** Word文档解析器实例 */
    private final WordAbstractParser wordParser;

    /** Excel文档解析器实例 */
    private final ExcelAbstractParser excelParser;

    /** HTML文档解析器实例 */
    private final HtmlAbstractParser htmlParser;

    /**
     * 默认构造函数
     * 初始化各种文件格式的默认解析器实例
     */
    public TemplateParser() {
        this.wordParser = new WordParser();
        this.excelParser = new ExcelParser();
        this.htmlParser = new HtmlParser();
    }

    /**
     * 带参数的构造函数
     * 允许外部传入自定义的解析器实例
     *
     * @param wordParser  Word文档解析器实例
     * @param excelParser Excel文档解析器实例
     * @param htmlParser  HTML文档解析器实例
     */
    public TemplateParser(WordAbstractParser wordParser, ExcelAbstractParser excelParser, HtmlAbstractParser htmlParser) {
        this.wordParser = wordParser;
        this.excelParser = excelParser;
        this.htmlParser = htmlParser;
    }

    /**
     * 获取Word文档解析器实例
     *
     * @return WordAbstractParser Word文档解析器对象
     */
    public WordAbstractParser getWordParser() {
        return this.wordParser;
    }

    /**
     * 获取Excel文档解析器实例
     *
     * @return ExcelAbstractParser Excel文档解析器对象
     */
    public ExcelAbstractParser getExcelParser() {
        return this.excelParser;
    }

    /**
     * 获取HTML文档解析器实例
     *
     * @return HtmlAbstractParser HTML文档解析器对象
     */
    public HtmlAbstractParser getHtmlParser() {
        return this.htmlParser;
    }

    /**
     * 根据传入的Word文档数据和参数映射，解析并生成新的Word文档。
     *
     * @param data 原始Word文档的字节数组
     * @param map 用于替换或填充文档内容的参数映射
     * @return 解析后生成的新Word文档字节数组
     */
    public byte[] parseWordByData(byte[] data, Map<String, Object> map) {
        try {
            return this.wordParser.parseByData(data, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据指定URL获取Word文档，并结合参数映射进行解析生成新文档。
     *
     * @param url Word文档的网络地址
     * @param map 用于替换或填充文档内容的参数映射
     * @return 解析后生成的新Word文档字节数组
     */
    public byte[] parsWordByUrl(String url, Map<String, Object> map)  {
        try {
            return this.wordParser.parseByUrl(url, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 根据传入的Excel文档数据和参数映射，解析并生成新的Excel文档。
     *
     * @param data 原始Excel文档的字节数组
     * @param map 用于替换或填充文档内容的参数映射
     * @return 解析后生成的新Excel文档字节数组
     */
    public byte[] parseExcelByData(byte[] data, Map<String, Object> map) {
        try {
            return this.excelParser.parseByData(data, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据指定URL获取Excel文档，并结合参数映射进行解析生成新文档。
     *
     * @param url Excel文档的网络地址
     * @param map 用于替换或填充文档内容的参数映射
     * @return 解析后生成的新Excel文档字节数组
     */
    public byte[] parseExcelByUrl(String url, Map<String, Object> map)  {
        try {
            return this.excelParser.parseByUrl(url, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据传入的HTML文档数据和参数映射，解析并生成新的HTML文档。
     *
     * @param data 原始HTML文档的字节数组
     * @param map 用于替换或填充文档内容的参数映射
     * @return 解析后生成的新HTML文档字节数组
     */
    public byte[] parseHtmlByData(byte[] data, Map<String, Object> map) {
        try {
            return this.htmlParser.parseByData(data, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据指定URL获取HTML文档，并结合参数映射进行解析生成新文档。
     *
     * @param url HTML文档的网络地址
     * @param map 用于替换或填充文档内容的参数映射
     * @return 解析后生成的新HTML文档字节数组
     */
    public byte[] parseHtmlByUrl(String url, Map<String, Object> map)  {
        try {
            return this.htmlParser.parseByUrl(url, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
