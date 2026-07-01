package com.xiongx9527.fw.plus.common.web;

/**
 * Web响应接口，定义了处理HTTP响应的相关方法
 */
public interface WebResponse {

    /**
     * 获取响应对象
     *
     * @return 响应对象
     */
    Object getResponse();

    /**
     * 设置响应状态码
     *
     * @param sc HTTP状态码
     */
    void setStatus(int sc);

    /**
     * 设置响应内容类型
     *
     * @param contentType 内容类型，如"text/html"、"application/json"等
     */
    void setContentType(String contentType);

    /**
     * 设置字符编码
     *
     * @param charset 字符编码格式，如"UTF-8"、"GBK"等
     */
    void setCharacterEncoding(String charset);

    /**
     * 设置日期类型的响应头
     *
     * @param name 头部名称
     * @param date 日期值（毫秒时间戳）
     */
    void setDateHeader(String name, long date);

    /**
     * 设置响应头信息
     *
     * @param name 头部名称
     * @param value 头部值
     */
    void setHeader(String name, String value);

    /**
     * 添加响应头信息（可重复添加同名头部）
     *
     * @param name 头部名称
     * @param value 头部值
     */
    void addHeader(String name, String value);

    /**
     * 将结果写入响应
     *
     * @param result 要写入的结果字符串
     * @return 处理后的对象
     * @throws Throwable 写入过程中可能抛出的异常
     */
    Object writeWith(String result) throws Throwable;

}

