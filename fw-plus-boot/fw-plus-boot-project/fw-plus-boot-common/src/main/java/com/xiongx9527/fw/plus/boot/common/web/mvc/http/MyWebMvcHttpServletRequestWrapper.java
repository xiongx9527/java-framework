package com.xiongx9527.fw.plus.boot.common.web.mvc.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public abstract class MyWebMvcHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public MyWebMvcHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }


}
