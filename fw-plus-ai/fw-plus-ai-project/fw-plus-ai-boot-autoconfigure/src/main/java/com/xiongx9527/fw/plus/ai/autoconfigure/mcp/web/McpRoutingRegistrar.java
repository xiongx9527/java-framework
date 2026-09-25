package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
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
		RequestMappingHandlerMapping handlerMapping = findHandlerMapping();
		if (handlerMapping == null) {
			return;
		}

		for (McpServerRuntime runtime : runtimes) {
			registerRuntime(handlerMapping, runtime);
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
		String endpoint = runtime.getEndpoint();
		if (endpoint == null || endpoint.isEmpty()) {
			throw new IllegalStateException(
					"MCP runtime '" + runtime.getKey() + "' has an empty endpoint, cannot register routes.");
		}

		if ("streamable-http".equalsIgnoreCase(runtime.getTransport())) {
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

		handlerMapping.registerMapping(builder.build(), controller, method);
	}
}
