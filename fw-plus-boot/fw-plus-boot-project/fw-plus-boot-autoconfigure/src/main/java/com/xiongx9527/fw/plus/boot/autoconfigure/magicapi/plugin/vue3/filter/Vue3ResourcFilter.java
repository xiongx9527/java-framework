package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.filter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules.VueModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class Vue3ResourcFilter extends Vue3ResourcBaseFilter implements Filter {

    /**
     * 魔法API前缀配置项
     * 用于指定魔法API的URL前缀路径
     * 从配置文件中读取magic-api.prefix属性值，如果未配置则默认为空字符串
     */
    @Value("${magic-api.prefix:}")
    private String magicApiPrefix;

    /**
     * Vue3资源过滤器类
     * 用于处理Vue3组件的动态加载和导出
     */
    private final VueModule vueModule;

    /**
     * 构造函数
     * @param vueModule Vue模块实例，用于创建可导出的组件
     */
    public Vue3ResourcFilter(VueModule vueModule) {
        this.vueModule = vueModule;
    }

    /**
     * 执行过滤器逻辑
     * 根据请求参数中的ID生成对应的Vue3组件JavaScript代码并返回
     * @param servletRequest Servlet请求对象，包含客户端请求信息
     * @param servletResponse Servlet响应对象，用于向客户端返回数据
     * @param filterChain 过滤器链，用于继续执行后续过滤器或目标资源
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String url = ((HttpServletRequest) servletRequest).getRequestURI();
        if (url.contains(".")) {
            int lastDotIndex = url.lastIndexOf(".");
            String path = url.substring(0, lastDotIndex);
            //String suffix = url.substring(lastDotIndex + 1);
            // 移除前缀
            if (path.startsWith(magicApiPrefix)) {
                path = path.substring(magicApiPrefix.length());
            }
            try {
                // 根据ID创建可导出的Vue组件
                String exportableComponent = vueModule.createExportableComponent(path);
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                // 设置响应头信息，指定内容类型为JavaScript，并配置跨域访问策略
                response.setContentType("application/javascript;charset=UTF-8");
                // 注意：不推荐设置过于开放的 CORS 策略，请按需调整
                response.setHeader("Access-Control-Allow-Origin", "*");
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                response.setHeader("Access-Control-Allow-Headers", "*");
                response.getWriter().write(exportableComponent);
                response.getWriter().flush();
                return ;
            } catch (Exception e) {
                log.warn("Vue3组件加载失败：{}", e.getMessage());
            }
        }
        // 如果没有找到对应的组件或出现异常，则继续执行过滤器链
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
