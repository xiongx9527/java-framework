package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webmvc;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
		"jakarta.servlet.http.HttpServletRequest",
		"org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping" })
public class McpRoutingRegistrar implements SmartInitializingSingleton {

	private final ApplicationContext context;

	private final List<McpServerRuntime> runtimes;

	private final Map<McpServerRuntime, List<RequestMappingInfo>> registeredMappings =
			new ConcurrentHashMap<McpServerRuntime, List<RequestMappingInfo>>();

	public McpRoutingRegistrar(ApplicationContext context, List<McpServerRuntime> runtimes) {
		this.context = context;
		this.runtimes = runtimes;
	}

	@Override
	public void afterSingletonsInstantiated() {
		RequestMappingHandlerMapping handlerMapping = findHandlerMapping();
		if (handlerMapping == null) {
			return;
		}

		for (McpServerRuntime runtime : runtimes) {
			registerRuntime(handlerMapping, runtime);
		}
	}

	/**
	 * 运行期新增实例时注册路由，语义与启动期一致。
	 *
	 * @param runtime 待注册 runtime
	 */
	public void registerRuntime(McpServerRuntime runtime) {
		RequestMappingHandlerMapping handlerMapping = findHandlerMapping();
		if (handlerMapping == null) {
			throw new IllegalStateException("RequestMappingHandlerMapping not available, cannot register MCP routes");
		}
		registerRuntime(handlerMapping, runtime);
	}

	/**
	 * 运行期移除实例时注销该实例的全部路由。
	 *
	 * @param runtime 待移除 runtime
	 */
	public void unregisterRuntime(McpServerRuntime runtime) {
		List<RequestMappingInfo> mappings = registeredMappings.remove(runtime);
		if (mappings == null) {
			return;
		}
		RequestMappingHandlerMapping handlerMapping = findHandlerMapping();
		if (handlerMapping == null) {
			return;
		}
		for (RequestMappingInfo mapping : mappings) {
			handlerMapping.unregisterMapping(mapping);
		}
	}

	/**
	 * 优先按 Spring MVC 标准 bean 名获取主 handlerMapping，避免与框架（如 knife4j/springdoc）
	 * 注册的扩展 handlerMapping 类型重合导致按类型注入出现多候选；取不到时静默跳过。
	 */
	private RequestMappingHandlerMapping findHandlerMapping() {
		if (context.containsBean("requestMappingHandlerMapping")) {
			return context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
		}
		try {
			return context.getBean(RequestMappingHandlerMapping.class);
		} catch (NoSuchBeanDefinitionException e) {
			return null;
		}
	}

	private void registerRuntime(RequestMappingHandlerMapping handlerMapping, McpServerRuntime runtime) {
		if (registeredMappings.containsKey(runtime)) {
			return;
		}
		List<RequestMappingInfo> mappings = new ArrayList<>();
		registeredMappings.put(runtime, mappings);
		String endpoint = runtime.getEndpoint();
		if (endpoint == null || endpoint.isEmpty()) {
			throw new IllegalStateException(
					"MCP runtime '" + runtime.getKey() + "' has an empty endpoint, cannot register routes.");
		}

		if ("streamable-http".equalsIgnoreCase(runtime.getTransport())) {
			McpStreamableHttpController controller = new McpStreamableHttpController(runtime);
			register(handlerMapping, mappings, controller, "post",
					new Class<?>[] { String.class, HttpServletRequest.class, HttpServletResponse.class },
					endpoint, RequestMethod.POST);
			register(handlerMapping, mappings, controller, "delete",
					new Class<?>[] { HttpServletRequest.class, HttpServletResponse.class },
					endpoint, RequestMethod.DELETE);
			register(handlerMapping, mappings, controller, "get",
					new Class<?>[] { HttpServletRequest.class, HttpServletResponse.class },
					endpoint, RequestMethod.GET);
		} else {
			McpSseController controller = new McpSseController(runtime);
			register(handlerMapping, mappings, controller, "streamSse",
					new Class<?>[] { HttpServletRequest.class, HttpServletResponse.class },
					endpoint + McpSseController.SSE_PATH, RequestMethod.GET, "text/event-stream");
			register(handlerMapping, mappings, controller, "handleMessage",
					new Class<?>[] { String.class, String.class, HttpServletRequest.class, HttpServletResponse.class },
					endpoint + McpSseController.MESSAGES_PATH, RequestMethod.POST);
		}
	}

	private void register(RequestMappingHandlerMapping handlerMapping, List<RequestMappingInfo> mappings,
			Object controller, String methodName, Class<?>[] parameterTypes, String pattern,
			RequestMethod requestMethod, String... produces) {
		Method method;
		try {
			method = controller.getClass().getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("MCP handler method not found: " + controller.getClass().getSimpleName()
					+ "#" + methodName, e);
		}

		RequestMappingInfo.BuilderConfiguration builderConfiguration = new RequestMappingInfo.BuilderConfiguration();
		builderConfiguration.setUrlPathHelper(handlerMapping.getUrlPathHelper());
		builderConfiguration.setPathMatcher(handlerMapping.getPathMatcher());
		builderConfiguration.setTrailingSlashMatch(handlerMapping.useTrailingSlashMatch());
		// 主 handlerMapping 默认使用 PathPatternParser（Spring Boot 2.6+），其 initLookupPath
		// 会移除 UrlPathHelper.PATH 属性并改用 PathContainer；若此处构建 ant 条件将导致匹配异常，
		// 因此将 patternParser 一并传入，让 build 按需生成与 handlerMapping 一致的匹配条件。
		builderConfiguration.setPatternParser(handlerMapping.getPatternParser());

		RequestMappingInfo.Builder builder = RequestMappingInfo.paths(pattern).methods(requestMethod);
		for (String producesType : produces) {
			builder.produces(producesType);
		}
		builder.options(builderConfiguration);

		RequestMappingInfo mapping = builder.build();
		mappings.add(mapping);
		handlerMapping.registerMapping(mapping, controller, method);
	}
}
