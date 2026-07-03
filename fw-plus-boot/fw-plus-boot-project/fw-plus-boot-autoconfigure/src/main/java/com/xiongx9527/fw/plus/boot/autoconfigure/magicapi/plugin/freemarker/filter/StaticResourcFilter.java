package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.core.Ordered;

public class StaticResourcFilter extends StaticResourcBaseFilter implements Filter , Ordered {
    /**
     * 将脚本内容写入HTTP响应中
     *
     * @param response HTTP响应对象，用于返回脚本内容给客户端
     * @param suffix 脚本文件后缀名，用于设置响应的内容类型
     * @param script 要返回的脚本内容
     */
    @Override
    void writeScript(Object response, String suffix, String script) {
        HttpServletResponse res = (HttpServletResponse) response;
        // 设置响应内容类型，并以文本格式返回脚本内容
        res.setContentType(String.format("text/%s;charset=UTF-8", suffix));
        // 添加浏览器缓存控制头
        ((HttpServletResponse) response).setHeader("Cache-Control", "max-age=3600, public");
        //响应浏览器
        try {
            res.getWriter().write(script);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行过滤器逻辑，处理HTTP请求并执行过滤器链
     *
     * @param request  Servlet请求对象，必须是HttpServletRequest类型
     * @param response Servlet响应对象
     * @param chain    过滤器链，用于执行下一个过滤器或目标资源
     * @throws IOException      当IO操作出现异常时抛出
     * @throws ServletException 当Servlet操作出现异常时抛出
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 确保是HTTP请求
        if (!(request instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("Only HTTP requests are supported");
        }

        // 处理请求URI并写入脚本
        if(writeScript(((HttpServletRequest) request).getRequestURI(), response)) {
            return;
        }

        // 执行过滤器链中的下一个元素
        chain.doFilter(request, response);
    }


    /**
     * 获取过滤器的执行顺序
     * 
     * @return 返回Integer.MAX_VALUE，表示该过滤器应该在所有其他过滤器之后执行
     */
    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
