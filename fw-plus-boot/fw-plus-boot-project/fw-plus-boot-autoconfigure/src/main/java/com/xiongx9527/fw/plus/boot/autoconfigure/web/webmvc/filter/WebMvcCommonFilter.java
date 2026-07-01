package com.xiongx9527.fw.plus.boot.autoconfigure.web.webmvc.filter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.filter.WebCommonFilter;
import com.xiongx9527.fw.plus.boot.common.web.ICommonFilterService;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptor;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import com.xiongx9527.fw.plus.boot.common.web.mvc.filter.WebMvcBaseRequestFilter;
import com.xiongx9527.fw.plus.boot.common.web.mvc.http.DecryptWebMvcHttpServletRequestWrapper;
import com.xiongx9527.fw.plus.boot.common.web.mvc.http.EncryptWebMvcHttpServletResponseWrapper;
import com.xiongx9527.fw.plus.common.util.WebUtils;
import com.xiongx9527.fw.plus.common.web.WebMvcRequestContextHolder;
import com.xiongx9527.fw.plus.common.web.WebRequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@WebFilter(urlPatterns = "/*")
@Slf4j
public class WebMvcCommonFilter extends WebMvcBaseRequestFilter implements WebCommonFilter , Ordered {

    /**
     * 执行过滤操作的核心方法
     * 该方法主要负责在请求处理前后执行过滤操作，以及在请求处理后根据响应状态进行日志记录
     *
     * @param request 代表HTTP请求，用于获取请求信息
     * @param response 代表HTTP响应，用于获取响应状态
     * @param filterChain 过滤链，用于将请求传递给下一个过滤器或目标资源
     * @throws ServletException 如果过滤过程中发生Servlet异常
     * @throws IOException 如果过滤过程中发生I/O异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    		throws ServletException, IOException {
        ICommonFilterService webCommonFilterService = this.getWebCommonFilterService();
        MyHttpEncryptor encryptor = this.getMyHttpEncryptor();
        MyHttpEncryptorProperties encryptorProperties = this.getMyHttpEncryptorProperties();
        boolean enabled = (Objects.nonNull(encryptorProperties) && encryptorProperties.isEnabled())
                && Objects.nonNull(encryptor) && encryptor.isGolabal();
        if (enabled && Objects.nonNull(encryptorProperties.getExcludePaths())) {
            String requestPath = request.getServletPath();
            for (String url : encryptorProperties.getExcludePaths()) {
                if (WebUtils.defaultPathMatcher().match(url, requestPath)) {
                    enabled = false;
                    break;
                }
            }
        }
        HttpServletRequest requestWrapper = enabled && encryptor.isGolabal() ?
                new DecryptWebMvcHttpServletRequestWrapper(request, encryptor, encryptorProperties):request;
        HttpServletResponse responseWrapper = ((requestWrapper instanceof DecryptWebMvcHttpServletRequestWrapper)
                && ((DecryptWebMvcHttpServletRequestWrapper)requestWrapper).isNeedEncrypt())
                && encryptor.isGolabal()?
                new EncryptWebMvcHttpServletResponseWrapper(response, encryptor, encryptorProperties):response;
        // 声明变量用于存储请求的查找路径
        String lookupPath = null;
        try {
            // 解析并缓存请求的查找路径，以便后续使用
            lookupPath = webCommonFilterService.resolveAndCacheLookupPath();
            // 在请求处理前执行过滤操作
            webCommonFilterService.doFilterBefore(new WebMvcRequestContextHolder.WebMvcRequest(requestWrapper), new WebMvcRequestContextHolder.WebMvcResponse(responseWrapper));
        } finally {
            try {
                // 将请求传递给下一个过滤器或目标资源
                filterChain.doFilter(requestWrapper, responseWrapper);
            } finally {
                // 获取HTTP响应状态码
                int status = responseWrapper.getStatus();
                // 如果响应状态码不是200（OK）或0（未设置状态码），则记录警告日志
                if (status != HttpStatus.OK.value() && status != HttpStatus.NOT_MODIFIED.value() && status != 0
                    && !WebUtils.isWebSocketRequest(WebRequestContextHolder.getWebMvcRequest(requestWrapper))) {
                    log.warn("error http response status. path={}, status={}", lookupPath, status);
                } else if (responseWrapper instanceof EncryptWebMvcHttpServletResponseWrapper) {
                    try {
                        ((EncryptWebMvcHttpServletResponseWrapper)responseWrapper).encryptAndWrite();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                // 在请求处理后执行过滤操作
                webCommonFilterService.doFilterAfter();

            }
        }
    }

    /**
     * 获取排序顺序值
     *
     * @return 返回一个极小的整数值，用于确保该组件在排序中具有最低优先级
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }

}
