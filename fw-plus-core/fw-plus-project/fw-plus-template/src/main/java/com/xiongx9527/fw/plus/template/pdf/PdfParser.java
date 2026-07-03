package com.xiongx9527.fw.plus.template.pdf;

import java.io.IOException;
import java.util.Map;
/**
 * PdfParser类实现了PdfAbstractParser接口，用于解析PDF文件和对PDF文件进行转换操作。
 *
 * <p>该类作为PDF解析器的具体实现，负责解析PDF文件的内容并根据参数进行替换。
 * 它利用FreeMarker模板引擎处理PDF模板，支持将PDF内容转换为HTML格式。</p>
 *
 * @since 1.0
 */
public class PdfParser extends PdfAbstractParser {
    /**
     * PdfParser构造函数
     *
     * 该构造函数用于创建PdfParser对象的实例
     */
    public PdfParser() {
    }

    /**
     * 根据输入数据和配置映射表进行解析处理
     *
     * @param data 待解析的字节数组数据
     * @param map 配置映射表，包含解析所需的配置参数
     * @return 解析处理后的字节数组结果
     * @throws Exception 解析过程中可能抛出的异常
     */
    @Override
    public byte[] parseByData(byte[] data, Map<String, Object> map) throws Exception {
        return parse(data, map, null);
    }

    /**
     * 根据输入数据、配置映射表和配置路径进行解析处理
     *
     * @param data 待解析的字节数组数据
     * @param map 配置映射表，包含解析所需的配置参数
     * @param configPath 配置文件路径，可为null
     * @return 解析处理后的字节数组结果
     * @throws RuntimeException 当解析过程中发生IO异常时转换为运行时异常抛出
     */
    @Override
    public byte[] parseByData(byte[] data, Map<String, Object> map, String configPath) {
        try {
            // 调用底层解析方法处理数据
            return parse(data, map, configPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将PDF格式转换为HTML格式
     * @param srcBytes 源PDF文件的字节数组
     * @return 转换后的HTML字节数组
     * @throws UnsupportedOperationException 当前功能未实现，抛出此异常
     */
    @Override
    public byte[] toHmtl(byte[] srcBytes) {
        // 抛出异常表明当前功能未实现
        throw new UnsupportedOperationException("Pdf to Html conversion is not supported");
    }


    /**
     * 将源字节数组转换为PDF格式的字节数组
     *
     * @param srcBytes 源数据的字节数组
     * @return 转换后的PDF格式字节数组
     * @throws UnsupportedOperationException 当前功能未实现，始终抛出此异常
     */
    @Override
    public byte[] toPdf(byte[] srcBytes) {
        // 抛出异常表明当前功能未实现
        throw new UnsupportedOperationException("Pdf to Pdf conversion is not supported");
    }

}
