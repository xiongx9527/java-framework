package com.xiongx9527.fw.plus.boot.autoconfigure.report.webmvc.jimu;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 积木报表系统切换过滤器
 * 用于检测系统中是否包含积木报表或积木仪表盘组件
 */
@WebFilter(urlPatterns = {"jmreport/list","/drag/list"})
public class JimuReportSysSwitchFilter implements Filter {

    // 是否包含积木报表组件
    boolean includeJimuReport = false;
    // 是否包含积木仪表盘组件
    boolean includeJimuDrag = false;

    /**
     * 初始化过滤器
     * @param filterConfig 过滤器配置
     * @throws ServletException 如果初始化失败
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
        // 通过类加载判断是否包含积木报表
        try {
            Class.forName("org.jeecg.modules.jmreport.config.JmReportBaseConfig");
            includeJimuReport = true;
        } catch (ClassNotFoundException ignored) {
        }
        // 通过类加载判断是否包含积木仪表盘
        try {
            Class.forName("org.jeecg.modules.drag.service.IOnlDragCompService");
            includeJimuDrag = true;
        } catch (ClassNotFoundException ignored) {
        }
    }

    /**
     * 执行过滤逻辑
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException 如果发生I/O错误
     * @throws ServletException 如果处理请求时发生错误
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession();
            // 设置积木报表可用状态到session
            // 设置积木仪表盘可用状态到session
            if(includeJimuReport) {
                session.setAttribute("switchJimuReport", "true");
            }else{
                session.setAttribute("switchJimuReport", "false");
            }
            if(includeJimuDrag) {
                session.setAttribute("switchJimuDrag", "true");
            }else {
                session.setAttribute("switchJimuDrag", "false");
            }
        }
        chain.doFilter(request, response);
    }


}
