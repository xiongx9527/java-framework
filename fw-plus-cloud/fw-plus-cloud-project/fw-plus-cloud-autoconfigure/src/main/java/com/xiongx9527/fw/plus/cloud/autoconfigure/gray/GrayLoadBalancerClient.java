
// GrayEnhancedLoadBalancerClient.java
package com.xiongx9527.fw.plus.cloud.autoconfigure.gray;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xiongx9527.fw.plus.common.gray.GrayContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrayLoadBalancerClient implements LoadBalancerClient {

    /** 委托的标准负载均衡客户端 */
    private final LoadBalancerClient delegate;

    /** 服务发现客户端，用于获取可用的服务实例列表 */
    private final DiscoveryClient discoveryClient;

    /** 服务实例缓存，用于提高性能 */
    private final Cache<String, List<ServiceInstance>> instancesCache = CacheBuilder.newBuilder()
            .maximumSize(1000)  // 最大缓存1000个服务
            .expireAfterWrite(30, TimeUnit.SECONDS)  // 30秒后过期
            .build();

    /**
     * 构造方法。
     *
     * @param delegate         标准负载均衡客户端实现
     * @param discoveryClient  服务发现客户端实现
     */
    public GrayLoadBalancerClient(LoadBalancerClient delegate, DiscoveryClient discoveryClient) {
        this.delegate = delegate;
        this.discoveryClient = discoveryClient;
    }

    /**
     * 执行指定服务的负载均衡请求。
     *
     * @param serviceId 服务ID
     * @param request   负载均衡请求
     * @param <T>       返回结果类型
     * @return 请求执行结果
     * @throws IOException IO异常
     */
    @Override
    public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
        return this.delegate.execute(serviceId, request);
    }

    /**
     * 使用指定服务实例执行负载均衡请求。
     *
     * @param serviceId       服务ID
     * @param serviceInstance 指定的服务实例
     * @param request         负载均衡请求
     * @param <T>             返回结果类型
     * @return 请求执行结果
     * @throws IOException IO异常
     */
    @Override
    public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {
        return this.delegate.execute(serviceId, serviceInstance, request);
    }

    /**
     * 根据原始URI和服务实例重建目标URI。
     *
     * @param instance 目标服务实例
     * @param original 原始请求URI
     * @return 重构后的URI
     */
    @Override
    public URI reconstructURI(ServiceInstance instance, URI original) {
        return this.delegate.reconstructURI(instance, original);
    }

    /**
     * 根据服务ID选择一个服务实例。
     *
     * @param serviceId 服务ID
     * @return 选中的服务实例，若无可用实例则返回null
     */
    @Override
    public ServiceInstance choose(String serviceId) {
        return this.choose(serviceId, ReactiveLoadBalancer.REQUEST);
    }


    /**
     * 获取指定服务的所有可用实例（带缓存）
     *
     * @param serviceId 服务ID
     * @return 服务实例列表，如果无实例则返回空列表
     */
    private List<ServiceInstance> getInstances(String serviceId) {
        try {
            // 使用双重同步机制确保线程安全的缓存访问
            synchronized (serviceId) {
                return instancesCache.get(serviceId, () -> {
                    synchronized (serviceId) {
                        // 从服务发现客户端获取服务实例列表
                        List<ServiceInstance> instances = this.discoveryClient.getInstances(serviceId);
                        return instances != null ? instances : new ArrayList<>();
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to get instances for service: {}", serviceId, e);
            return null;
        }
    }


    /**
     * 根据服务ID和请求上下文选择一个服务实例。
     * <p>
     * 本方法会首先尝试从服务注册中心获取所有可用实例，并依据当前是否处于灰度环境进行筛选：
     * - 若在灰度环境中，则优先选取与当前灰度版本及标签匹配的实例；
     * - 否则优先选取普通实例；
     * - 如果上述条件均未满足，则回退至委托客户端的默认负载均衡策略。
     *
     * @param serviceId 服务ID
     * @param request   当前请求上下文（可为空）
     * @param <T>       请求体泛型类型
     * @return 匹配的服务实例或通过默认策略选出的实例；若无可选实例则返回null
     */
    @Override
    public <T> ServiceInstance choose(String serviceId, Request<T> request) {
        // 获取指定服务的所有可用实例
        List<ServiceInstance> instances = getInstances(serviceId);
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        // 如果只有一个实例，直接返回
        if (instances.size() == 1) {
            return instances.get(0);
        }

        // 获取当前灰度上下文信息
        String grayVersion = GrayContext.getVersion();
        String grayTag = GrayContext.getTag();

        if (StringUtils.isNotBlank(grayVersion) && StringUtils.isNotBlank(grayTag)) {
            // 灰度环境：查找并返回匹配的灰度实例
            ServiceInstance grayInstance = findGrayInstance(instances, grayVersion, grayTag);
            if (grayInstance != null) {
                return grayInstance;
            }
        } else {
            // 非灰度环境：查找并返回普通实例
            ServiceInstance normalInstance = findNormalInstance(instances);
            if (normalInstance != null) {
                return normalInstance;
            }
        }

        // 所有前置条件都不满足时，使用默认负载均衡策略选择实例
        return this.delegate.choose(serviceId, request);
    }

    /**
     * 查找匹配的灰度实例
     */
    private ServiceInstance findGrayInstance(List<ServiceInstance> instances, String version, String tag) {
        try {
            if (instances != null && !instances.isEmpty()) {
                // 筛选匹配的灰度实例
                List<ServiceInstance> matchedInstances = new ArrayList<>();
                for (ServiceInstance instance : instances) {
                    if (isMatchGrayCriteria(instance, version, tag)) {
                        matchedInstances.add(instance);
                    }
                }

                // 如果有匹配的实例，随机返回一个
                if (!matchedInstances.isEmpty()) {
                    return matchedInstances.get(new Random().nextInt(matchedInstances.size()));
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 查找非灰度实例
     */
    private ServiceInstance findNormalInstance(List<ServiceInstance> instances) {
        try {
            if (instances != null && !instances.isEmpty()) {
                // 筛选非灰度实例
                List<ServiceInstance> normalInstances = new ArrayList<>();
                for (ServiceInstance instance : instances) {
                    if (!isGrayInstance(instance)) {
                        normalInstances.add(instance);
                    }
                }

                // 如果有非灰度实例，随机返回一个
                if (!normalInstances.isEmpty()) {
                    return normalInstances.get(new Random().nextInt(normalInstances.size()));
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 判断服务实例是否匹配灰度条件
     */
    private boolean isMatchGrayCriteria(ServiceInstance instance, String version, String tag) {
        if (instance.getMetadata() == null) {
            return false;
        }

        String instanceVersion = instance.getMetadata().get(GrayContext.GRAY_VERSION_HEADER);
        String instanceTag = instance.getMetadata().get(GrayContext.GRAY_TAG_HEADER);

        return StringUtils.equals(version, instanceVersion) &&
                StringUtils.equals(tag, instanceTag);
    }

    /**
     * 判断是否为灰度实例
     */
    private boolean isGrayInstance(ServiceInstance instance) {
        if (instance.getMetadata() == null) {
            return false;
        }

        String instanceVersion = instance.getMetadata().get(GrayContext.GRAY_VERSION_HEADER);
        String instanceTag = instance.getMetadata().get(GrayContext.GRAY_TAG_HEADER);

        return StringUtils.isNotBlank(instanceVersion) && StringUtils.isNotBlank(instanceTag);
    }
}
