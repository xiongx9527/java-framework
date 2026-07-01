package com.xiongx9527.fw.plus.common.web;

import org.springframework.http.HttpInputMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

/**
 * Web请求接口，定义了处理Web请求的相关方法
 */
public interface WebRequest {

    /**
     * 获取原始请求对象
     *
     * @return 原始请求对象
     */
    Object getRequest();

    /**
     * 获取Web会话对象
     *
     * @return Web会话对象
     */
    WebSession getSession();

    /**
     * 根据名称获取请求头信息
     *
     * @param name 请求头名称
     * @return 请求头的值，如果不存在则返回null
     */
    String getHeader(String name);

    /**
     * 获取远程客户端IP地址
     *
     * @return 远程客户端IP地址
     */
    String getRemoteAddr();

    /**
     * 获取请求输入流
     *
     * @return 请求输入流
     * @throws IOException 当IO操作出现异常时抛出
     */
    InputStream getInputStream() throws IOException;

    /**
     * 获取请求内容长度
     *
     * @return 请求内容长度，如果未知则返回-1
     */
    int getContentLength();

    /**
     * 获取请求内容类型
     *
     * @return 请求内容类型
     */
    String getContentType();


    /**
     * 获取所有请求头名称的枚举对象
     *
     * @return 包含所有请求头名称的枚举对象
     */
    Enumeration<String> getHeaderNames();

    /**
     * 根据名称获取所有匹配的请求头信息
     *
     * @param name 请求头名称
     * @return 包含所有匹配请求头值的枚举对象
     */
    Enumeration<String> getHeaders(String name) ;

    /**
     * 设置请求属性
     *
     * @param key 属性键
     * @param value 属性值
     */
    void setAttribute(String key, Object value) ;

    /**
     * 获取请求URI
     *
     * @return 请求URI
     */
    String getRequestURI() ;

    /**
     * 获取请求方法
     *
     * @return 请求方法（如GET、POST等）
     */
    String getMethod() ;

    /**
     * 根据参数名称获取参数值数组
     *
     * @param name 参数名称
     * @return 参数值数组，如果参数不存在则返回null
     */
    String[] getParameterValues(String name) ;

    /**
     * 根据属性名称获取请求属性
     *
     * @param name 属性名称
     * @return 属性值，如果属性不存在则返回null
     */
    Object getAttribute(String name) ;

    /**
     * 获取HTTP输入消息对象
     *
     * @return HTTP输入消息对象
     */
    HttpInputMessage getHttpInputMessage() ;

    /**
     * 获取所有Cookie对象数组
     *
     * @return Cookie对象数组
     */
    WebCookie[] getCookies();

    /**
     * 根据路径获取请求分发器
     *
     * @param path 分发路径
     * @return 请求分发器对象
     */
    WebRequestContextHolder.WebRequestDispatcher getRequestDispatcher(String path);
}

