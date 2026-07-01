package com.xx9527.fw.plus.cloud.autoconfigure.magicapi.modules.http;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.sse.SseHttpModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.sse.SseHttpProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.net.URI;

/**
 * 自定义 SSE HTTP 模块（支持负载均衡）
 * 
 * <p>该类继承自 SseHttpModule，集成了 Spring Cloud 负载均衡功能。
 * 在建立 SSE 连接时，能够自动通过服务名从注册中心获取实际的服务实例地址，
 * 适用于微服务架构下的 SSE 通信场景。</p>
 */
@Slf4j
@MagicModule("ssehttp")
public class MySseHttpModule extends SseHttpModule {

    /**
     * Spring Cloud 负载均衡客户端
     */
    private final LoadBalancerClient loadBalancerClient;

    /**
     * 构造函数
     * 
     * <p>创建 MySseHttpModule 实例，初始化 SSE 配置和负载均衡客户端。</p>
     *
     * @param sseHttpProperties SSE HTTP 配置属性
     * @param loadBalancerClient Spring Cloud 负载均衡客户端
     */
    public MySseHttpModule(SseHttpProperties sseHttpProperties, LoadBalancerClient loadBalancerClient) {
        super(sseHttpProperties);
        this.loadBalancerClient = loadBalancerClient;
    }

    /**
     * 连接到目标 URL（支持负载均衡）
     * 
     * <p>该方法重写了父类的 connect 方法，增加了负载均衡支持。
     * 当传入的 URL 中包含服务名时，会通过 LoadBalancerClient 从注册中心获取实际的服务实例地址，
     * 并将 URL 中的主机名替换为实际的 IP 和端口。如果负载均衡失败或出现异常，则使用原始 URL 进行连接。</p>
     *
     * @param url 目标 URL，可以是服务名格式（如 http://service-name/path）或完整的服务实例地址
     * @return SseHttpModule 实例，用于后续的 SSE 请求操作
     */
    public SseHttpModule connect(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (StringUtils.isNotBlank(host)) {
                // 通过负载均衡客户端选择服务实例
                ServiceInstance serviceInstance = this.loadBalancerClient.choose(host);
                if (null != serviceInstance) {
                    // 替换URL中的主机名为实际的服务实例地址
                    String tmpUrl = url.replace(host, serviceInstance.getHost()+":"+serviceInstance.getPort());
                    return super.connect(tmpUrl);
                }
            }
        } catch (Exception e) {
            log.warn("Load-balancer resolution failed for URL: {}, falling back to direct connect", url, e);
        }
        // 如果负载均衡失败或出现异常，使用原始URL进行连接
        return super.connect(url);
    }

}
