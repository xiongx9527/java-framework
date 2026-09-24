package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;

/**
 * MVC 传输路由注册器。
 * <p>
 * 所有单例 bean 就绪后，为每个 runtime 编程式注册一套路径到
 * {@link RequestMappingHandlerMapping}（与注解路径等价，但 endpoint 可在运行期解析）：
 * </p>
 * <ul>
 *     <li>http-sse：{@code GET {endpoint}/sse}、{@code POST {endpoint}/messages/{uuid}}</li>
 *     <li>streamable-http：{@code POST/GET/DELETE {endpoint}}</li>
 * </ul>
 * <p>路径唯一性（同 transport）由 {@code McpRuntimeFactory} 在构建期保证，
 * 冲突时启动即失败而非注册期静默覆盖。</p>
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(name = {
		"javax.servlet.http.HttpServletRequest",
		"org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping" })
public class McpRoutingRegistrar implements SmartInitializingSingleton {

	private final ApplicationContext context;

	private final List<McpServerRuntime> runtimes;

	public McpRoutingRegistrar(ApplicationContext context, List<McpServerRuntime> runtimes) {
		this.context = context;
		this.runtimes = runtimes;
	}

	@Override
	public void afterSingletonsInstantiated() {
		RequestMappingHandlerMapping handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
		if (handlerMapping == null) {
			return;
		}

		for (McpServerRuntime runtime : runtimes) {
			registerRuntime(handlerMapping, runtime);
		}
	}

	private void registerRuntime(RequestMappingHandlerMapping handlerMapping, McpServerRuntime runtime) {
		String endpoint = runtime.getEndpoint();
		if (endpoint == null || endpoint.isEmpty()) {
			throw new IllegalStateException(
					"MCP runtime '" + runtime.getKey() + "' has an empty endpoint, cannot register routes.");
		}

		if ("streamable-http".equals(runtime.getTransport())) {
			McpStreamableHttpController controller = new McpStreamableHttpController(runtime);
			register(handlerMapping, controller, "post",
					new Class<?>[] { String.class, HttpServletRequest.class, HttpServletResponse.class },
					endpoint, RequestMethod.POST);
			register(handlerMapping, controller, "delete",
					new Class<?>[] { HttpServletRequest.class, HttpServletResponse.class },
					endpoint, RequestMethod.DELETE);
			register(handlerMapping, controller, "get",
					new Class<?>[] { HttpServletRequest.class, HttpServletResponse.class },
					endpoint, RequestMethod.GET);
		} else {
			McpSseController controller = new McpSseController(runtime);
			register(handlerMapping, controller, "streamSse",
					new Class<?>[] { HttpServletRequest.class, HttpServletResponse.class },
					endpoint + McpSseController.SSE_PATH, RequestMethod.GET, "text/event-stream");
			register(handlerMapping, controller, "handleMessage",
					new Class<?>[] { String.class, String.class, HttpServletRequest.class, HttpServletResponse.class },
					endpoint + McpSseController.MESSAGES_PATH, RequestMethod.POST);
		}
	}

	private void register(RequestMappingHandlerMapping handlerMapping, Object controller, String methodName,
			Class<?>[] parameterTypes, String pattern, RequestMethod requestMethod, String... produces) {
		Method method;
		try {
			method = controller.getClass().getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("MCP handler method not found: " + controller.getClass().getSimpleName()
					+ "#" + methodName, e);
		}

		RequestMappingInfo.Builder builder = RequestMappingInfo.paths(pattern).methods(requestMethod);
		for (String producesType : produces) {
			builder.produces(producesType);
		}

		handlerMapping.registerMapping(builder.build(), controller, method);
	}
}
