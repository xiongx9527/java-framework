package com.xiongx9527.fw.plus.common.web;

/**
 * Web会话接口，用于管理Web应用程序中的会话信息
 */
public interface WebSession {

    /**
     * 获取会话对象
     *
     * @return 返回底层的会话实现对象
     */
    Object getSession();

    /**
     * 根据键名获取会话属性值
     *
     * @param key 属性键名
     * @return 返回与指定键关联的属性值，如果不存在则返回null
     */
    Object getAttribute(String key);

    /**
     * 设置会话属性
     *
     * @param key 属性键名
     * @param value 要设置的属性值
     */
    void setAttribute(String key, Object value) ;
}

