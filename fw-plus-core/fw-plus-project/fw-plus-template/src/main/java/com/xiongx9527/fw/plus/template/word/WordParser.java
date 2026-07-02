package com.xiongx9527.fw.plus.template.word;

import cn.afterturn.easypoi.word.entity.MyXWPFDocument;
import cn.afterturn.easypoi.word.parse.ParseWord07;
import com.xiongx9527.fw.plus.template.docx4j.Docx4jWord2Html;
import com.xiongx9527.fw.plus.template.docx4j.Docx4jWord2Pdf;
import com.xiongx9527.fw.plus.template.filechange.AbstractWord2Html;
import com.xiongx9527.fw.plus.template.filechange.AbstractWord2Pdf;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
/**
 * WordParser类实现了WordAbstractParser接口，用于解析Word文件和对Word文件进行转换操作。
 *
 * <p>该类作为Word解析器的具体实现，负责解析Word文件的内容并根据参数进行替换。
 * 它利用FreeMarker模板引擎处理Word模板，支持将Word内容转换为HTML格式。</p>
 *
 * @since 1.0
 */
@Slf4j
public class WordParser extends WordAbstractParser {

    /**
     * Word07文档解析器实例
     *
     * 该字段用于创建和初始化Word 2007格式文档的解析器对象。
     * ParseWord07类负责处理.docx格式文件的解析逻辑。
     */
    private final ParseWord07 parseWord07 = new ParseWord07();

    /**
     * Word文档解析器
     * 用于将Word文档转换为HTML或PDF格式
     */
    private final AbstractWord2Html word2Html;

    /**
     * Word文档转换为PDF处理器
     */
    private final AbstractWord2Pdf word2Pdf;

    /**
     * 默认构造函数
     * 初始化默认的Word转PDF和Word转HTML处理器
     */
    public WordParser() {
        this.word2Pdf = new Docx4jWord2Pdf();
        this.word2Html = new Docx4jWord2Html();
    }

    /**
     * 带参数的构造函数
     * 允许外部注入自定义的Word处理实现
     *
     * @param word2Html Word转HTML处理器
     * @param word2Pdf Word转PDF处理器
     */
    public WordParser(AbstractWord2Html word2Html, AbstractWord2Pdf word2Pdf) {
        super();
        this.word2Html = word2Html;
        this.word2Pdf = word2Pdf;
    }

    /**
     * 将XWPFDocument文档转换为字节数组
     *
     * @param document 要转换的XWPFDocument文档对象
     * @return 包含文档内容的字节数组
     * @throws IOException 当文档写入过程中发生IO异常时抛出
     */
    private byte[] convertXWPFDocumentToByteArray(XWPFDocument document) throws IOException {
        // 使用try-with-resources确保资源自动关闭
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            // 将文档内容写入字节输出流
            document.write(byteArrayOutputStream);
            // 返回字节数组
            return byteArrayOutputStream.toByteArray();
        }
    }

    /**
     * 根据字节数组数据创建MyXWPFDocument对象
     *
     * @param data 包含文档数据的字节数组，用于创建XWPFDocument对象
     * @return 成功时返回MyXWPFDocument对象，失败时返回null
     */
    private MyXWPFDocument getXWPFDocument(byte[] data) {
        // 使用字节数组创建输入流，并尝试创建MyXWPFDocument对象
        try (InputStream is = new ByteArrayInputStream(data)) {
            return new MyXWPFDocument(is);
        } catch (Exception e) {
            // 记录创建XWPFDocument对象失败的日志信息
            log.error("创建XWPFDocument对象失败，数据长度：{}", data != null ? data.length : 0, e);
            return null;
        }
    }

    /**
     * 解析数据并根据提供的映射关系处理Word文档
     *
     * @param data 原始Word文档数据字节数组
     * @param map 包含替换变量和对应值的映射关系
     * @return 处理后的Word文档字节数组
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public byte[] parseByData(byte[] data, Map<String, Object> map) throws Exception {
        // 获取Word文档对象
        MyXWPFDocument document = getXWPFDocument(data);
        // 解析Word文档内容
        this.parseWord07.parseWord(document, map);
        // 将处理后的文档转换为字节数组并返回
        return convertXWPFDocumentToByteArray(document);
    }

    /**
     * 将源字节数组转换为HTML格式
     * @param srcBytes 源文件的字节数组
     * @return 转换后的HTML格式字节数组
     * @throws RuntimeException 当转换过程中发生异常时抛出
     */
    @Override
    public byte[] toHmtl(byte[] srcBytes) {
        try {
            // 调用word2Pdf组件进行格式转换
            return this.word2Pdf.change(srcBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将源字节数组转换为PDF格式
     * @param srcBytes 源文件的字节数组
     * @return 转换后的PDF格式字节数组
     * @throws RuntimeException 当转换过程中发生异常时抛出
     */
    @Override
    public byte[] toPdf(byte[] srcBytes) {
        try {
            // 调用word2Html组件进行格式转换
            return word2Html.change(srcBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
