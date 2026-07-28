package com.xiongx9527.fw.plus.common.web;

import com.xiongx9527.fw.plus.common.util.StringUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class WebMvcRequestContextHolder {
    private WebMvcRequestContextHolder(){

    }

    public static WebMvcRequest getRequest() {
        // 使用RequestContextHolder获取当前线程的请求属性
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 判断获取到的请求属性是否为空，如果不为空，则返回对应的HttpServletRequest对象
        HttpServletRequest request = servletRequestAttributes == null ? null : servletRequestAttributes.getRequest();
        return new WebMvcRequest(request);
    }

    public static WebMvcResponse getResponse() {
        // 使用RequestContextHolder获取当前线程的请求属性
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 判断获取到的请求属性是否为空，如果不为空，则返回对应的HttpServletRequest对象
        HttpServletResponse response = servletRequestAttributes == null ? null : servletRequestAttributes.getResponse();
        return new WebMvcResponse(response);
    }


    public static class WebMvcSession implements WebSession {
        private HttpSession httpSession;

        private WebMvcSession(){}

        public WebMvcSession(HttpSession httpSession) {
            this.httpSession = httpSession;
        }
        @Override
        public Object getSession() {
            return this.httpSession;
        }
        @Override
        public Object getAttribute(String name) {
            return null==this.httpSession?null:this.httpSession.getAttribute(name);
        }

        @Override
        public void setAttribute(String key, Object value) {
            if (null!=this.httpSession) {
                this.httpSession.setAttribute(key, value);
            }
        }
    }



    public  static class WebMvcRequest implements WebRequest {

        private HttpServletRequest request;

        private WebMvcRequest(){}

        public WebMvcRequest(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public Object getRequest() {
            return this.request;
        }

        @Override
        public WebSession getSession() {
            return null!=this.request?new WebMvcSession(this.request.getSession()):null;
        }

        @Override
        public String getHeader(String name) {
            if (StringUtils.isEmpty(name)) {
                return null;
            }
            return null!=this.request?this.request.getHeader(name):null;
        }

        @Override
        public String getRemoteAddr() {
            return null!=this.request?this.request.getRemoteAddr():null;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return null!=this.request?this.request.getInputStream():null;
        }

        @Override
        public int getContentLength() {
            return null!=this.request?this.request.getContentLength():null;
        }

        @Override
        public String getContentType() {
            return null!=this.request?this.request.getContentType():null;
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return null!=this.request?this.request.getHeaderNames():null;
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (null!=this.request) {
                return this.request.getHeaders(name);
            }
            return null;
        }

        @Override
        public void setAttribute(String key, Object value) {
            if (null!=this.request) {
                this.request.setAttribute(key, value);
            }
        }

        @Override
        public String getRequestURI() {
            if (null!=this.request) {
                return this.request.getRequestURI();
            }
            return "";
        }

        @Override
        public String getMethod() {
            if (null!=this.request) {
                return this.request.getMethod();
            }
            return "";
        }

        @Override
        public String[] getParameterValues(String name) {
            if (null!=this.request) {
                return this.request.getParameterValues(name);
            }
            return null;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            if (null!=this.request) {
                return this.request.getParameterNames();
            }
            return Collections.emptyEnumeration();
        }

        @Override
        public Object getAttribute(String name) {
            if (null!=this.request) {
                return this.request.getAttribute(name);
            }
            return null;
        }

        @Override
        public HttpInputMessage getHttpInputMessage() {
            if (null!=this.request) {
                return new HttpInputMessage() {
                    @Override
                    public InputStream getBody() throws IOException {
                        return getInputStream();
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        //todo
                        return new HttpHeaders();
                    }
                };
            }
            return null;
        }

        @Override
        public WebCookie[] getCookies() {
            if (null!=this.request) {
                Cookie[] cookies = this.request.getCookies();
                return cookies == null ? new WebCookie[0] : (WebCookie[])Arrays.stream(cookies).map(WebMvcCookie::new).toArray((x$0) -> {
                    return new WebMvcCookie[x$0];
                });
            }

            return null;
        }

        @Override
        public WebRequestContextHolder.WebRequestDispatcher getRequestDispatcher(String path) {
            if (null!=this.request) {
                return new WebMvcRequestDispatcher(path);
            }
            return null;
        }

    }

    public  static class WebMvcCookie implements WebCookie {
        private final Cookie cookie;

        public WebMvcCookie(Cookie cookie) {
            this.cookie = cookie;
        }

        @Override
        public Object getCookie() {
            return this.cookie;
        }
    }


    public  static class WebMvcResponse implements WebResponse {
        private HttpServletResponse response;

        private WebMvcResponse(){}

        public WebMvcResponse(HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public Object getResponse() {
            return this.response;
        }

        @Override
        public void setStatus(int sc) {
            if (null!=this.response) {
                this.response.setStatus(HttpStatus.valueOf(sc).value());
            }
        }

        @Override
        public void setContentType(String contentType) {
            if (null!=this.response) {
                this.response.setContentType(contentType);
            }
        }

        @Override
        public void setCharacterEncoding(String charset) {
            if (null!=this.response) {
                this.response.setCharacterEncoding(charset);
            }
        }

        @Override
        public void setDateHeader(String name, long date) {
            if (null!=this.response) {
                this.response.setDateHeader(name, date);
            }
        }

        @Override
        public void setHeader(String name, String value) {
            if (null!=this.response) {
                this.response.setHeader(name, value);
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if (null!=this.response) {
                this.response.addHeader(name, value);
            }
        }

        @Override
        public Object writeWith(String result) throws IOException {
            if (null!=this.response) {
                this.response.getWriter().write(result);
                return true;
            }
            return false;
        }
    }


    public static class WebMvcRequestDispatcher implements WebRequestContextHolder.WebRequestDispatcher {

        private String path;

        private WebMvcRequestDispatcher(){}

        public WebMvcRequestDispatcher(String path) {
            this.path = path;
        }

        private void setAttribute(HttpServletRequest request,Map<String, ?> newModel) {
            newModel.forEach((key, value) -> request.setAttribute(key, value));
        }

        @Override
        public void forward(WebRequest request, WebResponse response) throws Throwable {
            setAttribute((HttpServletRequest)request.getRequest(),new HashMap<>());
            ((HttpServletRequest)request.getRequest()).getRequestDispatcher(path)
                    .forward((HttpServletRequest)request.getRequest(),
                            (HttpServletResponse)response.getResponse());
        }

        @Override
        public void forward(WebRequest request, WebResponse response,Map<String, ?> model) throws Throwable {
            setAttribute((HttpServletRequest)request.getRequest(),MapUtils.emptyIfNull(model));
            ((HttpServletRequest)request.getRequest()).getRequestDispatcher(path)
                    .forward((HttpServletRequest)request.getRequest(),
                            (HttpServletResponse)response.getResponse());
        }

        @Override
        public void include(WebRequest request, WebResponse response) throws Throwable {
            setAttribute((HttpServletRequest)request.getRequest(),new HashMap<>());
            ((HttpServletRequest)request.getRequest()).getRequestDispatcher(path)
                    .include((HttpServletRequest)request.getRequest(),
                            (HttpServletResponse)response.getResponse());
        }

        @Override
        public void include(WebRequest request, WebResponse response, Map<String, ?> model) throws Throwable {
            setAttribute((HttpServletRequest)request.getRequest(),MapUtils.emptyIfNull(model));
            ((HttpServletRequest)request.getRequest()).getRequestDispatcher(path)
                    .include((HttpServletRequest)request.getRequest(),
                            (HttpServletResponse)response.getResponse());
        }
    }


}
