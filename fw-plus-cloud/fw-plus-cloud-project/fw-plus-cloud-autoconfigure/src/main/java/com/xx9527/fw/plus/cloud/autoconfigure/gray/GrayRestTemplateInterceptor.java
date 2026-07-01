package com.xx9527.fw.plus.cloud.autoconfigure.gray;

import com.xx9527.fw.plus.common.gray.GrayContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class GrayRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 拦截HTTP请求，在请求头中添加灰度发布相关的标识信息
     *
     * @param request HTTP请求对象，用于获取和修改请求头信息
     * @param body 请求体字节数组
     * @param execution HTTP请求执行器，用于继续执行请求链
     * @return ClientHttpResponse HTTP响应对象
     * @throws IOException IO异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // 如果当前处于灰度环境，则在请求头中添加版本号和标签信息
        if(GrayContext.isGray()) {
            request.getHeaders().add(GrayContext.GRAY_VERSION_HEADER, GrayContext.getVersion());
            request.getHeaders().add(GrayContext.GRAY_TAG_HEADER, GrayContext.getTag());
        }
        return execution.execute(request, body);
    }

}
