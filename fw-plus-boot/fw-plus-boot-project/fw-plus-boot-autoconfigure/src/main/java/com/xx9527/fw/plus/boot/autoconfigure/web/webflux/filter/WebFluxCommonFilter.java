package com.xx9527.fw.plus.boot.autoconfigure.web.webflux.filter;

import com.xx9527.fw.plus.boot.autoconfigure.web.filter.WebCommonFilter;
import com.xx9527.fw.plus.boot.common.web.ICommonFilterService;
import com.xx9527.fw.plus.boot.common.web.MyHttpEncryptor;
import com.xx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import com.xx9527.fw.plus.boot.common.web.flux.filter.WebFluxBaseRequestFilter;
import com.xx9527.fw.plus.boot.common.web.flux.http.DecryptWebFluxServerHttpRequestDecorator;
import com.xx9527.fw.plus.boot.common.web.flux.http.EncryptWebFluxServerHttpResponDecorator;
import com.xx9527.fw.plus.common.util.WebUtils;
import com.xx9527.fw.plus.common.web.WebFluxRequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

//@WebFilter(urlPatterns = "/*")
@Slf4j
public class WebFluxCommonFilter extends WebFluxBaseRequestFilter implements WebCommonFilter , Ordered {

    /**
     * 执行内部过滤逻辑，用于在请求处理前后进行加解密、日志记录等通用操作。
     *
     * @param exchange 当前的服务器Web交换对象，封装了请求和响应信息
     * @param chain 过滤器链，用于继续执行下一个过滤器或目标处理器
     * @return 返回一个Mono<Void>信号，表示异步操作完成
     */
    @Override
    protected Mono<Void> doFilterInternal(ServerWebExchange exchange, WebFilterChain chain)  {
        ICommonFilterService webCommonFilterService = this.getWebCommonFilterService();
        Mono<Void> mono = null;
        // 声明变量用于存储请求的查找路径
        String lookupPath = null;
        ServerWebExchange defaultServerWebExchange = exchange;
        try {
            WebFluxRequestContextHolder.set(exchange);
            MyHttpEncryptor encryptor = this.getMyHttpEncryptor();
            MyHttpEncryptorProperties encryptorProperties = this.getMyHttpEncryptorProperties();
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // 判断是否启用全局加密功能，并排除特定路径
            boolean enabled = (Objects.nonNull(encryptorProperties) && encryptorProperties.isEnabled())
                    && Objects.nonNull(encryptor) && encryptor.isGolabal();
            if (enabled && Objects.nonNull(encryptorProperties.getExcludePaths())) {
                String requestPath = request.getPath().value();
                for (String url : encryptorProperties.getExcludePaths()) {
                    if (WebUtils.defaultPathMatcher().match(url, requestPath)) {
                        enabled = false;
                        break;
                    }
                }
            }

            // 根据配置决定是否包装请求和响应以支持加解密
            ServerHttpRequest requestWrapper = enabled && encryptor.isGolabal() ?
                    new DecryptWebFluxServerHttpRequestDecorator(request, encryptor, encryptorProperties):request;
            ServerHttpResponse responseWrapper = ((requestWrapper instanceof DecryptWebFluxServerHttpRequestDecorator)
                    && ((DecryptWebFluxServerHttpRequestDecorator)requestWrapper).isNeedEncrypt())
                    && encryptor.isGolabal()?
                    new EncryptWebFluxServerHttpResponDecorator(response, encryptor, encryptorProperties):response;

            // 解析并缓存请求的查找路径，以便后续使用
            lookupPath = webCommonFilterService.resolveAndCacheLookupPath();
            // 创建新的 ServerWebExchange 对象
            defaultServerWebExchange = exchange.mutate()
                    .request(requestWrapper)
                    .response(responseWrapper)
                    .build();
            // 在请求处理前执行过滤操作
            webCommonFilterService.doFilterBefore(new WebFluxRequestContextHolder.WebFluxRequest(defaultServerWebExchange), new WebFluxRequestContextHolder.WebFluxResponse(defaultServerWebExchange));
        } finally {
            try {
                mono = chain.filter(defaultServerWebExchange);
            } finally {
                try {
                    // 获取HTTP响应状态码
                    HttpStatus statusCode = defaultServerWebExchange.getResponse().getStatusCode();
                    int status = statusCode != null ? statusCode.value() : 0;
                    // 如果响应状态码不是200（OK）或0（未设置状态码），则记录警告日志
                    if (status != HttpStatus.OK.value() && status != HttpStatus.NOT_MODIFIED.value() && status != 0) {
                        log.warn("error http response status. path={}, status={}", lookupPath, status);
                    }
                    // 在请求处理后执行过滤操作
                    webCommonFilterService.doFilterAfter();
                } finally {
                    WebFluxRequestContextHolder.clear();
                }
            }
        }
        return mono;
    }


    /**
     * 获取排序值
     *
     * @return 返回一个极小的整数值，用于确保该组件在排序中具有最低优先级
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 100;
    }


}
