package com.xx9527.fw.plus.cloud.autoconfigure.magicapi.modules.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.client.RestTemplate;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.modules.http.HttpModule;
import org.ssssssss.script.annotation.Comment;

import java.net.URI;


@Slf4j
@MagicModule("http")
public class MyHttpModule extends HttpModule {

    /**
     * 负载均衡客户端实例
     *
     * 用于执行服务间的负载均衡调用，通过该客户端可以获取服务实例并进行远程调用
     */
    private final LoadBalancerClient loadBalancerClient;

    /**
     * 构造函数，初始化MyHttpModule实例
     *
     * @param template RestTemplate对象，用于HTTP请求处理
     * @param loadBalancerClient 负载均衡客户端，用于服务发现和负载均衡
     */
    public MyHttpModule(RestTemplate template, LoadBalancerClient loadBalancerClient) {
        super(template);
        this.loadBalancerClient = loadBalancerClient;
    }

    /**
     * 创建连接
     * @param url 目标URL
     * @return HttpModule HTTP模块实例
     */
    @Comment("创建连接")
    public HttpModule connect(@Comment(name = "url", value = "目标URL") String url) {
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
        }
        // 如果负载均衡失败或出现异常，使用原始URL进行连接
        return super.connect(url);
    }


}
