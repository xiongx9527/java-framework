package com.xiongx9527.fw.plus.cloud.autoconfigure.gray;

import org.apache.commons.collections4.MapUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.LoadBalancerFeignRequestTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@ConditionalOnProperty(prefix = "fw.plus.cloud.gray", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@ConditionalOnClass({LoadBalancerClient.class, DiscoveryClient.class})
public class GrayLoadBalancerAutoConfiguration {


    /**
     * 创建并配置一个 GrayLoadBalancerClient 实例，用于支持灰度负载均衡功能。
     * <p>
     * 该方法首先尝试从已有的 LoadBalancerClient 集合中随机选择一个实例；如果集合为空或不存在，
     * 则创建一个新的 BlockingLoadBalancerClient 实例作为默认客户端。
     *
     * @param discoveryClient 服务发现客户端，用于获取服务实例信息
     * @param loadBalancerClientMap 已注册的负载均衡客户端映射表，键为名称，值为对应的客户端实现
     * @param loadBalancerClientFactory 负载均衡客户端工厂，用于构建新的负载均衡器实例
     * @return 返回封装了实际负载均衡逻辑的 GrayLoadBalancerClient 实例
     */
    @Primary
    @Bean
    public GrayLoadBalancerClient grayLoadBalancerClient(
            DiscoveryClient discoveryClient,
            Map<String,LoadBalancerClient> loadBalancerClientMap,
            ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
        LoadBalancerClient loadBalancerClient = null;
        // 尝试从已有客户端列表中随机选取一个
        if (MapUtils.isNotEmpty(loadBalancerClientMap) && loadBalancerClientMap.size() > 0) {
            List<LoadBalancerClient> loadBalancerClients = new ArrayList<>(loadBalancerClientMap.values());
            loadBalancerClient = loadBalancerClients.get(new Random().nextInt(loadBalancerClients.size()));
        } else {
            // 若无可用客户端，则使用默认阻塞式负载均衡客户端
            loadBalancerClient = new BlockingLoadBalancerClient(loadBalancerClientFactory);
        }
        return new GrayLoadBalancerClient(loadBalancerClient, discoveryClient);
    }

    /**
     * 创建支持重试功能的灰度负载均衡Feign客户端
     *
     * <p>该方法在以下条件下生效：
     * <ul>
     *   <li>classpath中存在RetryTemplate类</li>
     *   <li>classpath中存在feign.Client类</li>
     *   <li>classpath中存在EnableFeignClients类</li>
     * </ul>
     *
     * @param loadBalancerClient 负载均衡客户端，用于服务发现和负载均衡
     * @param discoveryClient 服务发现客户端，用于获取服务实例信息
     * @param loadBalancedRetryFactory 负载均衡重试工厂，提供重试策略
     * @param loadBalancerClientFactory 负载均衡客户端工厂，创建负载均衡器
     * @param transformers 请求转换器列表，用于处理请求转换
     * @return 支持重试功能的灰度负载均衡Feign客户端实例
     */
    @Primary
    @Bean
    @ConditionalOnClass(name={"org.springframework.retry.support.RetryTemplate",
            "feign.Client","org.springframework.cloud.openfeign.EnableFeignClients"})
    public GrayRetryableFeignBlockingLoadBalancerClient grayRetryableFeignBlockingLoadBalancerClient(
            LoadBalancerClient loadBalancerClient,
            DiscoveryClient discoveryClient,
            LoadBalancedRetryFactory loadBalancedRetryFactory,
            LoadBalancerClientFactory loadBalancerClientFactory,
            List<LoadBalancerFeignRequestTransformer> transformers) {
        return new GrayRetryableFeignBlockingLoadBalancerClient(new feign.Client.Default(null, null),
                loadBalancerClient,
                discoveryClient,
                loadBalancedRetryFactory,
                loadBalancerClientFactory,
                transformers);
    }

    /**
     * 创建不支持重试功能的灰度负载均衡Feign客户端
     *
     * <p>该方法在以下条件下生效：
     * <ul>
     *   <li>classpath中不存在RetryTemplate类</li>
     *   <li>classpath中存在feign.Client类</li>
     *   <li>classpath中存在EnableFeignClients类</li>
     * </ul>
     *
     * @param loadBalancerClient 负载均衡客户端，用于服务发现和负载均衡
     * @param discoveryClient 服务发现客户端，用于获取服务实例信息
     * @param loadBalancerClientFactory 负载均衡客户端工厂，创建负载均衡器
     * @param transformers 请求转换器列表，用于处理请求转换
     * @return 不支持重试功能的灰度负载均衡Feign客户端实例
     */
    @Primary
    @Bean
    @ConditionalOnMissingClass(value={"org.springframework.retry.support.RetryTemplate"})
    @ConditionalOnClass(name={"feign.Client","org.springframework.cloud.openfeign.EnableFeignClients"})
    public GrayFeignBlockingLoadBalancerClient grayFeignBlockingLoadBalancerClient(
            LoadBalancerClient loadBalancerClient,
            DiscoveryClient discoveryClient,
            LoadBalancerClientFactory loadBalancerClientFactory,
            List<LoadBalancerFeignRequestTransformer> transformers) {
        return new GrayFeignBlockingLoadBalancerClient(new feign.Client.Default(null, null),
                loadBalancerClient,
                discoveryClient,
                loadBalancerClientFactory,
                transformers);
    }


    /**
     * 创建灰度RestTemplate拦截器Bean
     * 该拦截器用于在使用RestTemplate进行HTTP请求时添加灰度相关的请求头信息
     *
     * @return 灰度RestTemplate拦截器实例
     */
    @Bean
    public GrayRestTemplateInterceptor grayRestTemplateInterceptor() {
        return new GrayRestTemplateInterceptor();
    }

    /**
     * 创建灰度Feign请求拦截器Bean
     * 该拦截器用于在使用Feign进行服务间调用时添加灰度相关的请求头信息
     *
     * @return 灰度Feign请求拦截器实例
     */
    @Bean
    public GrayFeignRequestInterceptor grayFeignRequestInterceptor() {
        return new GrayFeignRequestInterceptor();
    }

}
