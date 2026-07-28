package com.xiongx9527.fw.plus.common.web;

import com.xiongx9527.fw.plus.common.util.WebUtils;

import java.util.Map;

public class WebRequestContextHolder {

    /**
     * 私有构造函数，防止实例化该工具类
     */
    private WebRequestContextHolder(){}


    /**
     * 解析并缓存请求的查找路径
     *
     * @param request Web请求对象
     * @return 解析后的查找路径，如果无法解析则返回null
     */
    public static String resolveAndCacheLookupPath(WebRequest request) {
        if (request == null || request.getRequest() == null) {
            return null;
        }
        if (request.getRequest() instanceof javax.servlet.http.HttpServletRequest) {
            return WebUtils.defaultUrlPathHelper().resolveAndCacheLookupPath((javax.servlet.http.HttpServletRequest)request.getRequest());
        }
        return request.getRequestURI();
    }

    /**
     * 获取请求的查找路径
     *
     * @param request Web请求对象
     * @return 查找路径，如果无法获取则返回null
     */
    public static String getLookupPath(WebRequest request) {
        if (request == null || request.getRequest() == null) {
            return null;
        }
        if (request.getRequest() instanceof javax.servlet.http.HttpServletRequest) {
            return WebUtils.defaultUrlPathHelper().getResolvedLookupPath((javax.servlet.http.HttpServletRequest)request.getRequest());
        }
        return request.getRequestURI();
    }


    /**
     * 创建并返回一个WebRequest对象，用于封装HTTP请求的详细信息
     * 此方法主要用于将传统的HttpServletRequest请求转换为更易于在Web应用中使用的WebRequest对象
     *
     * @param request 传入的HttpServletRequest对象，包含了HTTP请求的所有信息
     * @return 返回一个WebRequest对象，该对象封装了传入的HttpServletRequest请求的所有信息
     */
    public static WebRequest getWebMvcRequest(javax.servlet.http.HttpServletRequest request) {
        return new WebMvcRequestContextHolder.WebMvcRequest(request);
    }

    /**
     * 将HttpServletResponse转换为WebResponse对象
     * 此方法用于在Web MVC环境中，将Servlet API的HttpServletResponse包装为更高级别的WebResponse对象，
     * 以便在框架内部以更抽象的方式处理HTTP响应，而不直接依赖于Servlet API
     *
     * @param response Servlet的HttpServletResponse对象，用于接收HTTP响应
     * @return 返回一个包装了HttpServletResponse的WebResponse对象
     */
    public static WebResponse getWebMvcResponse(javax.servlet.http.HttpServletResponse response) {
        return new WebMvcRequestContextHolder.WebMvcResponse(response);
    }


    /**
     * 创建并返回一个WebFluxRequest对象
     * 该方法用于将传入的ServerWebExchange对象包装成一个WebFluxRequest对象，以便在WebFluxRequestContextHolder中使用
     *
     * @param exchange ServerWebExchange对象，包含了所有请求和响应的信息
     * @return 返回一个WebFluxRequest对象，该对象包装了传入的exchange，用于在WebFluxRequestContextHolder中进行操作
     */
    public static WebRequest getWebFluxRequest(org.springframework.web.server.ServerWebExchange exchange) {
        return new WebFluxRequestContextHolder.WebFluxRequest(exchange);
    }

    /**
     * 获取WebFlux响应对象
     *
     * @param exchange 服务器Web交换对象，包含请求和响应的所有信息
     * @return 返回一个WebFluxResponse对象，封装了对WebFlux响应的处理
     */
    public static WebResponse getWebFluxResponse(org.springframework.web.server.ServerWebExchange exchange) {
        // 创建并返回一个WebFluxResponse对象，用于处理WebFlux中的响应
        return new WebFluxRequestContextHolder.WebFluxResponse(exchange);
    }

    /**
     * 获取当前线程的Web请求对象
     *
     * 此方法旨在根据不同Web技术（WebMvc或WebFlux）获取相应的请求上下文
     * 它首先检查当前环境使用的是哪种Web技术，然后 accordingly地获取请求
     * 如果当前环境既不是WebMvc也不是WebFlux环境，则返回null
     *
     * @return WebRequest对象，如果没有可用的请求上下文则返回null
     */
    public static WebRequest getRequest() {
        WebRequest request = getMvcRequestIfPresent();
        if (request != null && request.getRequest() != null) {
            return request;
        }
        request = getFluxRequestIfPresent();
        if (request != null && request.getRequest() != null) {
            return request;
        }
        // 如果都不是，返回null
        return null;
    }

    /**
     * 获取当前请求的响应对象
     *
     * 此方法根据当前Web框架的类型，动态获取响应对象它首先检查是否处于Web MVC框架环境中，
     * 如果是，则使用WebMvcRequestContextHolder获取响应对象；如果不在Web MVC环境中，
     * 则检查是否处于WebFlux框架环境中，如果是，则使用WebFluxRequestContextHolder获取响应对象
     * 如果当前环境既不是Web MVC也不是WebFlux，那么返回null
     *
     * @return WebResponse 当前请求的响应对象，如果无法确定当前框架类型或当前环境不支持Web请求，则返回null
     */
    public static WebResponse getResponse() {
        WebResponse response = getMvcResponseIfPresent();
        if (response != null && response.getResponse() != null) {
            return response;
        }
        response = getFluxResponseIfPresent();
        if (response != null && response.getResponse() != null) {
            return response;
        }
        // 如果当前环境既不是Web MVC也不是WebFlux，返回null
        return null;
    }

    private static WebRequest getMvcRequestIfPresent() {
        try {
            return WebMvcRequestContextHolder.getRequest();
        } catch (Throwable e) {
            return null;
        }
    }

    private static WebResponse getMvcResponseIfPresent() {
        try {
            return WebMvcRequestContextHolder.getResponse();
        } catch (Throwable e) {
            return null;
        }
    }

    private static WebRequest getFluxRequestIfPresent() {
        try {
            return WebFluxRequestContextHolder.getRequest();
        } catch (Throwable e) {
            return null;
        }
    }

    private static WebResponse getFluxResponseIfPresent() {
        try {
            return WebFluxRequestContextHolder.getResponse();
        } catch (Throwable e) {
            return null;
        }
    }


    public static interface WebRequestDispatcher  {

        void forward(WebRequest request, WebResponse response) throws Throwable;

        void forward(WebRequest request, WebResponse response, Map<String, ?> model) throws Throwable;

        void include(WebRequest request, WebResponse response) throws Throwable;

        void include(WebRequest request, WebResponse response, Map<String, ?> model) throws Throwable;
    }




}
