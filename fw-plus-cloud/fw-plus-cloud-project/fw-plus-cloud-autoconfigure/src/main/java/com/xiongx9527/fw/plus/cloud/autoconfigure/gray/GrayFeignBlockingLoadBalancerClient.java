package com.xiongx9527.fw.plus.cloud.autoconfigure.gray;

import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.LoadBalancerFeignRequestTransformer;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class GrayFeignBlockingLoadBalancerClient implements feign.Client{

    private static final Log LOG = LogFactory.getLog(GrayFeignBlockingLoadBalancerClient.class);

    private final Client delegate;

    //private final LoadBalancerClient loadBalancerClient;
    private final GrayLoadBalancerClient loadBalancerClient;

    private final LoadBalancerClientFactory loadBalancerClientFactory;

    private final List<LoadBalancerFeignRequestTransformer> transformers;

    public GrayFeignBlockingLoadBalancerClient(Client delegate, LoadBalancerClient loadBalancerClient, DiscoveryClient discoveryClient, LoadBalancerClientFactory loadBalancerClientFactory, List<LoadBalancerFeignRequestTransformer> transformers) {
        this.delegate = delegate;
        //this.loadBalancerClient = loadBalancerClient;
        this.loadBalancerClient = new GrayLoadBalancerClient(loadBalancerClient, discoveryClient);
        this.loadBalancerClientFactory = loadBalancerClientFactory;
        this.transformers = transformers;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        final URI originalUri = URI.create(request.url());
        String serviceId = originalUri.getHost();
        Assert.state(serviceId != null, "Request URI does not contain a valid hostname: " + originalUri);
        String hint = getHint(serviceId);
        DefaultRequest<RequestDataContext> lbRequest = new DefaultRequest<>(
                new RequestDataContext(LoadBalancerUtils.buildRequestData(request), hint));
        Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
                .getSupportedLifecycleProcessors(
                        loadBalancerClientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
                        RequestDataContext.class, ResponseData.class, ServiceInstance.class);
        supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
        ServiceInstance instance = loadBalancerClient.choose(serviceId, lbRequest);
        org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse = new DefaultResponse(
                instance);
        if (instance == null) {
            String message = "Load balancer does not contain an instance for the service " + serviceId;
            if (LOG.isWarnEnabled()) {
                LOG.warn(message);
            }
            supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
                    .onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
                            CompletionContext.Status.DISCARD, lbRequest, lbResponse)));
            return Response.builder()
                    .request(request)
                    .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                    .body(message, StandardCharsets.UTF_8)
                    .build();
        }
        String reconstructedUrl = loadBalancerClient.reconstructURI(instance, originalUri).toString();
        Request newRequest = buildRequest(request, reconstructedUrl, instance);
        return LoadBalancerUtils.executeWithLoadBalancerLifecycleProcessing(delegate, options, newRequest, lbRequest, lbResponse,
                supportedLifecycleProcessors);
    }

    protected Request buildRequest(Request request, String reconstructedUrl) {
        return Request.create(request.httpMethod(), reconstructedUrl, request.headers(), request.body(),
                request.charset(), request.requestTemplate());
    }

    protected Request buildRequest(Request request, String reconstructedUrl, ServiceInstance instance) {
        Request newRequest = buildRequest(request, reconstructedUrl);
        if (transformers != null) {
            for (LoadBalancerFeignRequestTransformer transformer : transformers) {
                newRequest = transformer.transformRequest(newRequest, instance);
            }
        }
        return newRequest;
    }

    // Visible for Sleuth instrumentation
    public Client getDelegate() {
        return delegate;
    }

    private String getHint(String serviceId) {
        LoadBalancerProperties properties = loadBalancerClientFactory.getProperties(serviceId);
        String defaultHint = properties.getHint().getOrDefault("default", "default");
        String hintPropertyValue = properties.getHint().get(serviceId);
        return hintPropertyValue != null ? hintPropertyValue : defaultHint;
    }
}
