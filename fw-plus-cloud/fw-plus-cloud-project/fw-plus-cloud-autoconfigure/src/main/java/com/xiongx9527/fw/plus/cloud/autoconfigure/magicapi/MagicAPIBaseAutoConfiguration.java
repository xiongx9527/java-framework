package com.xiongx9527.fw.plus.cloud.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.sse.SseHttpProperties;
import com.xiongx9527.fw.plus.cloud.autoconfigure.magicapi.modules.http.MyHttpModule;
import com.xiongx9527.fw.plus.cloud.autoconfigure.magicapi.modules.http.MySseHttpModule;
import com.xiongx9527.fw.plus.cloud.autoconfigure.magicapi.plugin.MagicApiPluginAutoConfiguration;
import org.apache.commons.collections4.MapUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name={"org.springframework.boot.restclient.RestTemplateBuilder",
        "org.springframework.cloud.client.loadbalancer.LoadBalancerClient",
        "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration"})
@AutoConfigureAfter(name = {
        // 插件配置带 @ConditionalOnBean(MagicResourceService.class)：servlet 下须在 magic-api starter
        // 之后评估；reactive 下须在 Boot 层 reactive 引擎之后评估，否则该 bean 尚未注册，插件不生效。
        "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration",
        "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration"
})
@Configuration
@Import({MagicApiPluginAutoConfiguration.class})
public class MagicAPIBaseAutoConfiguration {

    /**
     * 创建并配置MyHttpModule Bean实例
     *
     * @param restTemplateBuilder RestTemplate构建器，用于创建RestTemplate实例
     * @param interceptorMap 请求拦截器映射表，用于配置HTTP请求拦截器
     * @param loadBalancerClient 负载均衡客户端，用于实现客户端负载均衡
     * @return 配置完成的MyHttpModule实例
     */
    @Bean
    public MyHttpModule httpModule(org.springframework.boot.restclient.RestTemplateBuilder restTemplateBuilder,
                                   Map<String, ClientHttpRequestInterceptor> interceptorMap,
                                   org.springframework.cloud.client.loadbalancer.LoadBalancerClient loadBalancerClient) {
        // 创建RestTemplate实例用于HTTP请求
        RestTemplate restTemplate = restTemplateBuilder.build();

        // 配置消息转换器，支持UTF-8编码和所有媒体类型
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8) {
            {
                setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
            }
            @Override
            public boolean supports(Class<?> clazz) {
                return true;
            }
        });

        // 收集并配置请求拦截器
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        if (MapUtils.isNotEmpty(interceptorMap)) {
            for (ClientHttpRequestInterceptor interceptor : interceptorMap.values()) {
                interceptors.add(interceptor);
            }
        }
        return new MyHttpModule(restTemplate, loadBalancerClient);
    }


    /**
     * 创建 SSE HTTP 模块 Bean（支持负载均衡）
     * 
     * <p>该方法用于创建并配置 MySseHttpModule 实例，集成 Spring Cloud 负载均衡功能。
     * 仅在类路径中存在 DifyClientBuilder 时才会自动配置此 Bean。</p>
     *
     * @param sseHttpProperties SSE HTTP 配置属性，包含超时、请求头等配置
     * @param loadBalancerClient Spring Cloud 负载均衡客户端，用于服务发现和负载均衡
     * @return MySseHttpModule 实例，用于处理 SSE 请求
     */
    @Bean
    @ConditionalOnClass(name="com.xiongx9527.fw.plus.dify.DifyClientBuilder")
    public MySseHttpModule sseHttpModule(SseHttpProperties sseHttpProperties,
                                   org.springframework.cloud.client.loadbalancer.LoadBalancerClient loadBalancerClient) {
        return new MySseHttpModule(sseHttpProperties, loadBalancerClient);
    }


}
