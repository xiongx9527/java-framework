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
import org.springframework.cloud.openfeign.loadbalancer.LoadBalancerResponseStatusCodeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.util.*;


public class GrayRetryableFeignBlockingLoadBalancerClient implements feign.Client{
    private static final Log LOG = LogFactory.getLog(GrayRetryableFeignBlockingLoadBalancerClient.class);

    private final feign.Client delegate;
    //private final LoadBalancerClient loadBalancerClient;
    private final GrayLoadBalancerClient loadBalancerClient;
    private final LoadBalancedRetryFactory loadBalancedRetryFactory;
    private final LoadBalancerClientFactory loadBalancerClientFactory;
    private final List<LoadBalancerFeignRequestTransformer> transformers;

    public GrayRetryableFeignBlockingLoadBalancerClient(feign.Client delegate, LoadBalancerClient loadBalancerClient, DiscoveryClient discoveryClient, LoadBalancedRetryFactory loadBalancedRetryFactory, LoadBalancerClientFactory loadBalancerClientFactory, List<LoadBalancerFeignRequestTransformer> transformers) {
        this.delegate = delegate;
        //this.loadBalancerClient = loadBalancerClient;
        this.loadBalancerClient = new GrayLoadBalancerClient(loadBalancerClient, discoveryClient);
        this.loadBalancedRetryFactory = loadBalancedRetryFactory;
        this.loadBalancerClientFactory = loadBalancerClientFactory;
        this.transformers = transformers;
    }

    public Response execute(Request request, Request.Options options) throws IOException {
        URI originalUri = URI.create(request.url());
        String serviceId = originalUri.getHost();
        Assert.state(serviceId != null, "Request URI does not contain a valid hostname: " + originalUri);
        LoadBalancedRetryPolicy retryPolicy = this.loadBalancedRetryFactory.createRetryPolicy(serviceId, this.loadBalancerClient);
        RetryTemplate retryTemplate = this.buildRetryTemplate(serviceId, request, retryPolicy);
        return (Response)retryTemplate.execute((context) -> {
            Request feignRequest = null;
            ServiceInstance retrievedServiceInstance = null;
            Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(this.loadBalancerClientFactory.getInstances(serviceId, LoadBalancerLifecycle.class), RetryableRequestContext.class, ResponseData.class, ServiceInstance.class);
            String hint = this.getHint(serviceId);
            DefaultRequest<RetryableRequestContext> lbRequest = new DefaultRequest(new RetryableRequestContext((ServiceInstance)null, LoadBalancerUtils.buildRequestData(request), hint));
            if (context instanceof LoadBalancedRetryContext lbContext) {
                retrievedServiceInstance = lbContext.getServiceInstance();
                if (retrievedServiceInstance == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Service instance retrieved from LoadBalancedRetryContext: was null. Reattempting service instance selection");
                    }

                    ServiceInstance previousServiceInstance = lbContext.getPreviousServiceInstance();
                    ((RetryableRequestContext)lbRequest.getContext()).setPreviousServiceInstance(previousServiceInstance);
                    supportedLifecycleProcessors.forEach((lifecycle) -> {
                        lifecycle.onStart(lbRequest);
                    });
                    retrievedServiceInstance = this.loadBalancerClient.choose(serviceId, lbRequest);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Selected service instance: %s", retrievedServiceInstance));
                    }

                    lbContext.setServiceInstance(retrievedServiceInstance);
                }

                if (retrievedServiceInstance == null) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Service instance was not resolved, executing the original request");
                    }

                    org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponsex = new DefaultResponse(retrievedServiceInstance);
                    supportedLifecycleProcessors.forEach((lifecycle) -> {
                        lifecycle.onComplete(new CompletionContext(CompletionContext.Status.DISCARD, lbRequest, lbResponsex));
                    });
                    feignRequest = request;
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Using service instance from LoadBalancedRetryContext: %s", retrievedServiceInstance));
                    }

                    String reconstructedUrl = this.loadBalancerClient.reconstructURI(retrievedServiceInstance, originalUri).toString();
                    feignRequest = this.buildRequest(request, reconstructedUrl, retrievedServiceInstance);
                }
            }

            org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse = new DefaultResponse(retrievedServiceInstance);
            this.loadBalancerClientFactory.getProperties(serviceId);
            Response response = LoadBalancerUtils.executeWithLoadBalancerLifecycleProcessing(this.delegate, options, feignRequest, lbRequest, lbResponse, supportedLifecycleProcessors, retrievedServiceInstance != null);
            int responseStatus = response.status();
            if (retryPolicy != null && retryPolicy.retryableStatusCode(responseStatus)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Retrying on status code: %d", responseStatus));
                }

                byte[] byteArray = response.body() == null ? new byte[0] : StreamUtils.copyToByteArray(response.body().asInputStream());
                response.close();
                throw new LoadBalancerResponseStatusCodeException(serviceId, response, byteArray, URI.create(request.url()));
            } else {
                return response;
            }
        }, new LoadBalancedRecoveryCallback<Response, Response>() {
            protected Response createResponse(Response response, URI uri) {
                return response;
            }
        });
    }

    protected Request buildRequest(Request request, String reconstructedUrl) {
        return Request.create(request.httpMethod(), reconstructedUrl, request.headers(), request.body(), request.charset(), request.requestTemplate());
    }

    protected Request buildRequest(Request request, String reconstructedUrl, ServiceInstance instance) {
        Request newRequest = this.buildRequest(request, reconstructedUrl);
        LoadBalancerFeignRequestTransformer transformer;
        if (this.transformers != null) {
            for(Iterator var5 = this.transformers.iterator(); var5.hasNext(); newRequest = transformer.transformRequest(newRequest, instance)) {
                transformer = (LoadBalancerFeignRequestTransformer)var5.next();
            }
        }

        return newRequest;
    }

    private RetryTemplate buildRetryTemplate(String serviceId, Request request, LoadBalancedRetryPolicy retryPolicy) {
        RetryTemplate retryTemplate = new RetryTemplate();
        BackOffPolicy backOffPolicy = this.loadBalancedRetryFactory.createBackOffPolicy(serviceId);
        retryTemplate.setBackOffPolicy((BackOffPolicy)(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy));
        RetryListener[] retryListeners = this.loadBalancedRetryFactory.createRetryListeners(serviceId);
        if (retryListeners != null && retryListeners.length != 0) {
            retryTemplate.setListeners(retryListeners);
        }

        retryTemplate.setRetryPolicy((RetryPolicy)(this.loadBalancerClientFactory.getProperties(serviceId).getRetry().isEnabled() && retryPolicy != null ? new InterceptorRetryPolicy(this.toHttpRequest(request), retryPolicy, this.loadBalancerClient, serviceId) : new NeverRetryPolicy()));
        return retryTemplate;
    }

    public Client getDelegate() {
        return this.delegate;
    }

    private HttpRequest toHttpRequest(final Request request) {
        return new HttpRequest() {
            public HttpMethod getMethod() {
                return HttpMethod.valueOf(request.httpMethod().name());
            }

            public URI getURI() {
                return URI.create(request.url());
            }

            public Map<String, Object> getAttributes() {
                Map<String, Object> attributes = new HashMap(request.requestTemplate().queries());
                return attributes;
            }

            public HttpHeaders getHeaders() {
                Map<String, List<String>> headers = new HashMap();
                Map<String, Collection<String>> feignHeaders = request.headers();
                Iterator var3 = feignHeaders.keySet().iterator();

                while(var3.hasNext()) {
                    String key = (String)var3.next();
                    headers.put(key, new ArrayList((Collection)feignHeaders.get(key)));
                }

                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(headers);
                return httpHeaders;
            }
        };
    }

    private String getHint(String serviceId) {
        LoadBalancerProperties properties = this.loadBalancerClientFactory.getProperties(serviceId);
        String defaultHint = (String)properties.getHint().getOrDefault("default", "default");
        String hintPropertyValue = (String)properties.getHint().get(serviceId);
        return hintPropertyValue != null ? hintPropertyValue : defaultHint;
    }

}
