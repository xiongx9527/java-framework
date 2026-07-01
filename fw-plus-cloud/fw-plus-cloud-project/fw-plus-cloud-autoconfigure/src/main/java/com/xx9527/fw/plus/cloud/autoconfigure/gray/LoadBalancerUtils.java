package com.xx9527.fw.plus.cloud.autoconfigure.gray;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import feign.Client;
import feign.Request;
import feign.Response;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * @author Olga Maciaszek-Sharma
 *
 * A utility class for handling {@link LoadBalancerLifecycle} calls.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
final class LoadBalancerUtils {

	private LoadBalancerUtils() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	static Response executeWithLoadBalancerLifecycleProcessing(Client feignClient, Request.Options options,
			Request feignRequest, org.springframework.cloud.client.loadbalancer.Request lbRequest,
			org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse,
			Set<LoadBalancerLifecycle> supportedLifecycleProcessors, boolean loadBalanced, boolean useRawStatusCodes)
			throws IOException {
		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, lbResponse));
		try {
			Response response = feignClient.execute(feignRequest, options);
			if (loadBalanced) {
				supportedLifecycleProcessors.forEach(
						lifecycle -> lifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.SUCCESS,
								lbRequest, lbResponse, buildResponseData(response, useRawStatusCodes))));
			}
			return response;
		}
		catch (Exception exception) {
			if (loadBalanced) {
				supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onComplete(
						new CompletionContext<>(CompletionContext.Status.FAILED, exception, lbRequest, lbResponse)));
			}
			throw exception;
		}
	}

	static ResponseData buildResponseData(Response response, boolean useRawStatusCodes) {
		HttpHeaders responseHeaders = new HttpHeaders();
		response.headers().forEach((key, value) -> responseHeaders.put(key, new ArrayList<>(value)));
		if (useRawStatusCodes) {
			return new ResponseData(responseHeaders, null, buildRequestData(response.request()), response.status());
		}
		return new ResponseData(HttpStatus.resolve(response.status()), responseHeaders, null,
				buildRequestData(response.request()));
	}

	static RequestData buildRequestData(Request request) {
		HttpHeaders requestHeaders = new HttpHeaders();
		request.headers().forEach((key, value) -> requestHeaders.put(key, new ArrayList<>(value)));
		return new RequestData(HttpMethod.resolve(request.httpMethod().name()), URI.create(request.url()),
				requestHeaders, null, new HashMap<>());
	}

	static Response executeWithLoadBalancerLifecycleProcessing(Client feignClient, Request.Options options,
			Request feignRequest, org.springframework.cloud.client.loadbalancer.Request lbRequest,
			org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse,
			Set<LoadBalancerLifecycle> supportedLifecycleProcessors, boolean useRawStatusCodes) throws IOException {
		return executeWithLoadBalancerLifecycleProcessing(feignClient, options, feignRequest, lbRequest, lbResponse,
				supportedLifecycleProcessors, true, useRawStatusCodes);
	}

}
