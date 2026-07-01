package com.xx9527.fw.plus.boot.common.web.mvc.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public abstract class MyWebMvcHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public MyWebMvcHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }


}
