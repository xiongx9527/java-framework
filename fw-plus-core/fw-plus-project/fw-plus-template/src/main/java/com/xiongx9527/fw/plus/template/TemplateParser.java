package com.xiongx9527.fw.plus.template;

import com.xiongx9527.fw.plus.template.excel.ExcelAbstractParser;
import com.xiongx9527.fw.plus.template.excel.ExcelParser;
import com.xiongx9527.fw.plus.template.html.HtmlAbstractParser;
import com.xiongx9527.fw.plus.template.html.HtmlParser;
import com.xiongx9527.fw.plus.template.pdf.PdfAbstractParser;
import com.xiongx9527.fw.plus.template.pdf.PdfParser;
import com.xiongx9527.fw.plus.template.word.WordAbstractParser;
import com.xiongx9527.fw.plus.template.word.WordParser;

import java.util.Map;
/**
 * TemplateParser类负责管理不同文件格式的解析器实例。
 *
 * <p>该类提供了对Word、Excel、HTML和PDF文件格式的解析操作，
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

    /** PDF文档解析器实例 */
    private final PdfAbstractParser pdfParser;

    /**
     * 默认构造函数
     * 初始化各种文件格式的默认解析器实例
     */
    public TemplateParser() {
        this.wordParser = new WordParser();
        this.excelParser = new ExcelParser();
        this.htmlParser = new HtmlParser();
        this.pdfParser = new PdfParser();
    }

    /**
     * 带参数的构造函数
     * 允许外部传入自定义的解析器实例
     *
     * @param wordParser  Word文档解析器实例
     * @param excelParser Excel文档解析器实例
     * @param htmlParser  HTML文档解析器实例
     * @param pdfParser   PDF文档解析器实例
     */
    public TemplateParser(WordParser wordParser, ExcelParser excelParser, HtmlParser htmlParser, PdfParser pdfParser) {
        this.wordParser = wordParser;
        this.excelParser = excelParser;
        this.htmlParser = htmlParser;
        this.pdfParser = pdfParser;
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
     * 获取PDF文档解析器实例
     *
     * @return PdfAbstractParser PDF文档解析器对象
     */
    public PdfAbstractParser getPdfParser() {
        return this.pdfParser;
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

    /**
     * 根据传入的PDF文档数据和参数映射，解析并生成新的PDF文档。
     *
     * @param data 原始PDF文档的字节数组
     * @param map 用于替换或填充文档内容的参数映射
     * @return 解析后生成的新PDF文档字节数组
     */
    public byte[] parsePdfByData(byte[] data, Map<String, Object> map) {
        try {
            return this.pdfParser.parseByData(data, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据传入的数据解析PDF文件
     *
     * @param data PDF文件的字节数组数据
     * @param map 解析配置参数映射表
     * @param configPath 配置文件路径
     * @return 解析后的PDF数据字节数组
     */
    public byte[] parsePdfByData(byte[] data, Map<String, Object> map,String configPath) {
        try {
            // 调用pdfParser的parseByData方法进行PDF解析
            return this.pdfParser.parseByData(data, map,configPath);
        } catch (Exception e) {
            // 将异常封装为运行时异常并抛出
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据指定URL获取PDF文档，并结合参数映射进行解析生成新文档。
     *
     * @param url PDF文档的网络地址
     * @param map 用于替换或填充文档内容的参数映射
     * @return 解析后生成的新PDF文档字节数组
     */
    public byte[] parsePdfByUrl(String url, Map<String, Object> map)  {
        try {
            return this.pdfParser.parseByUrl(url, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据URL解析PDF文件
     *
     * @param url PDF文件的URL地址
     * @param map 解析配置参数映射
     * @param configPath 配置文件路径
     * @return 解析后的PDF文件字节数组
     * @throws RuntimeException 当解析过程中发生异常时抛出
     */
    public byte[] parsePdfByUrl(String url, Map<String, Object> map, String configPath)  {
        try {
            // 调用pdfParser的parseByUrl方法进行PDF解析
            return this.pdfParser.parseByUrl(url, map,configPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
