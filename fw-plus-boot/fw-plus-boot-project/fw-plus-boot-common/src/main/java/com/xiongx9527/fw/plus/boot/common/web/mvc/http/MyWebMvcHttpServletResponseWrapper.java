package com.xiongx9527.fw.plus.boot.common.web.mvc.http;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public abstract class MyWebMvcHttpServletResponseWrapper extends HttpServletResponseWrapper {

    public MyWebMvcHttpServletResponseWrapper(HttpServletResponse response) {
        super(response);
    }

}
