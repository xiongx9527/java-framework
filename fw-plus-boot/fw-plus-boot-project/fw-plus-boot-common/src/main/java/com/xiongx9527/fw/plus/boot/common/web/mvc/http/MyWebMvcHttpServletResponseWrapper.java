package com.xiongx9527.fw.plus.boot.common.web.mvc.http;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

public abstract class MyWebMvcHttpServletResponseWrapper extends HttpServletResponseWrapper {

    public MyWebMvcHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

}
