package com.xiongx9527.fw.plus.template;

import cn.hutool.http.HttpUtil;

import java.util.Map;
/**
 * FileParser接口定义了文件解析操作的方法。
 *
 * <p>该接口包含了解析文件内容的方法，主要用于处理PDF和Word文件的解析操作。</p>
 *
 * @since 1.0
 */
public interface FileParser {


    /**
     * 解析数据并返回处理后的字节数组
     *
     * @param data 待解析的原始字节数据
     * @param map 包含解析配置或上下文信息的键值对集合
     * @return 解析处理后生成的字节数组结果
     * @throws Exception 当解析过程中发生错误时抛出异常
     */
    byte[] parseByData(byte[] data, Map<String, Object> map) throws Exception ;


    /**
     * 通过URL解析数据
     *
     * @param url 要下载数据的URL地址
     * @param map 解析时使用的参数映射
     * @return 解析后的字节数组结果
     * @throws Exception 当下载或解析过程中发生错误时抛出异常
     */
    default byte[] parseByUrl(String url, Map<String, Object> map) throws Exception  {
        return parseByData(HttpUtil.downloadBytes(url), map);
    }

}
