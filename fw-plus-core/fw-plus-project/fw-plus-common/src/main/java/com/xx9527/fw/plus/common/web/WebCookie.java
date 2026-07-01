package com.xx9527.fw.plus.common.web;

/**
 * WebCookie接口定义了获取Cookie对象的方法。
 * 该接口用于统一Web应用中Cookie的访问方式。
 */
public interface WebCookie {

    /**
     * 获取Cookie对象。
     *
     * @return 返回Cookie对象，具体类型取决于实现类
     */
    Object getCookie();
}

