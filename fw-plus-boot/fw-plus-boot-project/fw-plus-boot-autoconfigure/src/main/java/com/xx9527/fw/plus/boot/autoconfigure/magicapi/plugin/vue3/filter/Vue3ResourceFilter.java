package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.filter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules.VueModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class Vue3ResourceFilter extends Vue3ResourceBaseFilter implements Filter {
    private final MagicAPIProperties magicAPIProperties;

    private final VueModule vueModule;

    public Vue3ResourceFilter(MagicAPIProperties magicAPIProperties, VueModule vueModule) {
        this.magicAPIProperties = magicAPIProperties;
        this.vueModule = vueModule;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        // 非 GET 请求（如 POST /vue3/execute）直接放行给 Controller 处理
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        String url = request.getRequestURI();
        String web = magicAPIProperties.getWeb();
        if (web.endsWith("/")) {
            web = web.substring(0, web.length() - 1);
        }
        String magicApiPrefix = web + vueModule.getVue3ComponentPath();
        String path = url.substring(magicApiPrefix.length());
        try {
            String script = vueModule.createExportableComponent(path);
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            response.setContentType("application/javascript;charset=UTF-8");
            response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            response.getWriter().write(script);
            response.getWriter().flush();
        } catch (Exception e) {
            log.warn("Vue3组件加载失败：{}", e.getMessage());
            HttpServletResponse response = (HttpServletResponse) servletResponse;
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
