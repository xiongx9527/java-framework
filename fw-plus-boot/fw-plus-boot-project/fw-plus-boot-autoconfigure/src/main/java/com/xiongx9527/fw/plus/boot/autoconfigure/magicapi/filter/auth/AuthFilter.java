package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.filter.auth;

import com.alibaba.fastjson2.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.model.JsonBean;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//@WebFilter(value = "/resource/file/*/save")
public class AuthFilter implements Filter {

    // 静态常量，避免重复编译正则表达式
    private final Pattern readOnlyPattern;;

    /**
     * 是否启用只读模式
     * 从配置文件读取magic.api.resource.auth.read-only属性值，默认为false
     */
    @Value("${magic.api.resource.auth.read-only:false}")
    private boolean readOnly = true;

    /**
     * 需要排除的资源名称数组
     * 从配置文件读取magic.api.resource.auth.excludeResourceNames属性值，默认为空数组
     */
    @Value("${magic.api.resource.auth.excludeResourceNames:}")
    private String[] excludeResourceNames;

    /**
     * 构造函数，用于创建AuthFilter实例
     * @param properties MagicAPI的配置属性对象，用于获取Web基础路径配置
     */
    public AuthFilter(MagicAPIProperties properties) {
        String webBasePath = properties != null && properties.getWeb() != null ? properties.getWeb() : "";
        // 确保webBasePath以/开头但不以/结尾
        if (!webBasePath.startsWith("/") && !webBasePath.isEmpty()) {
            webBasePath = "/" + webBasePath;
        }
        if (webBasePath.endsWith("/")) {
            webBasePath = webBasePath.substring(0, webBasePath.length() - 1);
        }
        // 编译只读模式的正则表达式匹配模式
        try {
            this.readOnlyPattern = Pattern.compile(webBasePath + "/resource/file/([^/]+)/save");
        } catch (Exception e) {
            throw new RuntimeException("Failed to compile regex pattern", e);
        }
    }

        /**
     * 执行过滤器逻辑，用于控制资源的只读访问权限
     *
     * @param request  Servlet请求对象，包含客户端的请求信息
     * @param response Servlet响应对象，用于向客户端发送响应
     * @param chain    过滤器链，用于调用下一个过滤器或目标资源
     * @throws IOException      当发生I/O错误时抛出
     * @throws ServletException 当发生Servlet相关错误时抛出
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String url = req.getRequestURI();
        // 编译后的正则表达式缓存为常量
        Matcher matcher = this.readOnlyPattern.matcher(url);
        if (matcher.matches() && this.readOnly) {
            String resourceName = matcher.group(1); // 获取第一个捕获组的内容
            // 检查当前资源是否在排除列表中
            boolean isExcluded = false;
            if (this.excludeResourceNames != null && this.excludeResourceNames.length > 0) {
                for (String excludeResourceName : this.excludeResourceNames) {
                    if (resourceName.equals(excludeResourceName)) {
                        isExcluded = true;
                        break;
                    }
                }
            }

            // 如果资源不在排除列表中且为只读模式，则抛出异常
            if (!isExcluded) {
                HttpServletResponse res = (HttpServletResponse) response;
                res.setCharacterEncoding("UTF-8");
                res.setContentType("application/json;charset=UTF-8");
                JsonBean jsonBean = new JsonBean<>(JsonCodeConstants.IS_READ_ONLY);
                res.getWriter().write(JSON.toJSONString(jsonBean));
                return ;
            }
        }

        chain.doFilter(request, response);
    }


}
