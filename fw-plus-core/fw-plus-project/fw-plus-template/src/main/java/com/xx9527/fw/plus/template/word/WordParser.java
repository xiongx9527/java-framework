package com.xx9527.fw.plus.template.word;

import cn.afterturn.easypoi.word.entity.MyXWPFDocument;
import cn.afterturn.easypoi.word.parse.ParseWord07;
import com.xx9527.fw.plus.common.util.SpringUtils;
import com.xx9527.fw.plus.template.filechange.AbstractWord2Html;
import com.xx9527.fw.plus.template.filechange.AbstractWord2Pdf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        this.word2Pdf = null;
        this.word2Html = null;
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

    /** OLE2 (Office 97-2003 .doc) 格式的 magic bytes 签名 */
    private static final byte[] OLE2_MAGIC = {(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1};

    /**
     * 检测数据是否为 OLE2 格式（旧版 .doc 文件）
     *
     * @param data 文件字节数组
     * @return 如果是 OLE2 格式返回 true
     */
    private static boolean isOLE2Format(byte[] data) {
        if (data == null || data.length < OLE2_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < OLE2_MAGIC.length; i++) {
            if (data[i] != OLE2_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据字节数组数据创建MyXWPFDocument对象
     *
     * @param data 包含文档数据的字节数组，用于创建XWPFDocument对象
     * @return 成功时返回MyXWPFDocument对象，失败时返回null
     */
    private MyXWPFDocument getXWPFDocument(byte[] data) {
        if (isOLE2Format(data)) {
            log.error("不支持旧版 .doc 格式（OLE2），请将文档另存为 .docx（OOXML）格式后再试，数据长度：{}", data.length);
            return null;
        }
        // 使用字节数组创建输入流，并尝试创建MyXWPFDocument对象
        try (InputStream is = new ByteArrayInputStream(data)) {
            return new MyXWPFDocument(is);
        } catch (Exception e) {
            // 记录创建XWPFDocument对象失败的日志信息
            log.error("创建XWPFDocument对象失败，数据长度：{}", data.length, e);
            return null;
        }
    }

    /**
     * 根据提供的映射关系解析Word文档数据
     *
     * <p>该方法支持两种解析方式：
     * 1. 如果提供了beanName参数，则从Spring容器中获取对应的WordAbstractParser实现类实例进行解析
     * 2. 如果未提供beanName参数，则使用当前实例进行解析</p>
     *
     * @param beanName Spring容器中WordAbstractParser实现类的Bean名称，可为null或空字符串
     * @param data 原始Word文档的字节数组数据
     * @param map 包含模板变量名和对应替换值的映射关系，键为变量名，值为替换内容
     * @return 解析并替换后的Word文档字节数组
     * @throws Exception 当文档解析过程中发生错误时抛出异常
     */
    @Override
    public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) throws Exception {
        if (StringUtils.isNotBlank(beanName)) {
            return SpringUtils.getBean(beanName,WordAbstractParser.class).parseByData(data, map);
        }
        return this.parseByData(data, map);
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
        if (document == null) {
            throw new IllegalArgumentException("无法解析Word文档，请确认文件格式为 .docx（OOXML）");
        }
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
    public byte[] toHtml(byte[] srcBytes) {
        if (this.word2Html == null) {
            throw new UnsupportedOperationException("Word to Html conversion is not supported");
        }
        try {
            return this.word2Html.change(srcBytes);
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
        if (this.word2Pdf == null) {
            throw new UnsupportedOperationException("Word to Pdf conversion is not supported");
        }
        try {
            return this.word2Pdf.change(srcBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
