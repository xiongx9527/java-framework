package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebFlux 栈传输绑定。
 *
 * <p>内核是同步模型，这里按「同步入 Mono + 专用线程池」适配：请求处理在
 * {@link #mcpThreadPool} 上执行，SSE 出口用 Writer 桥把内核写出的帧原文转成
 * {@code Flux<String>} 推出，连接断开经 {@code sink.onDispose} 清理会话。</p>
 */
@Configuration
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnClass(name = "org.springframework.web.reactive.function.server.RouterFunctions")
public class McpWebfluxWebBinding {

	static final int DEFAULT_THREAD_POOL_SIZE = 16;

	static final String SSE_SUFFIX = "/sse";

	static final String MESSAGES_SUFFIX = "/messages/{uuid}";

	/**
	 * 专用线程池：把同步内核调用移出 Netty event loop，大小可配置。
	 */
	@Bean(name = "mcpThreadPool", destroyMethod = "shutdown")
	@ConditionalOnMissingBean(name = "mcpThreadPool")
	public ExecutorService mcpThreadPool(McpProperties properties) {
		int configured = properties == null ? 0 : properties.getThreadPoolSize();
		return Executors.newFixedThreadPool(configured > 0 ? configured : DEFAULT_THREAD_POOL_SIZE);
	}

	/**
	 * 每个 runtime 注册一套路由，router function bean 由 WebFlux 自动装配。
	 */
	@Bean
	@Primary
	public RouterFunction<ServerResponse> mcpRouterFunction(List<McpServerRuntime> runtimes,
			ExecutorService mcpThreadPool) {
		RouterFunction<ServerResponse> router = null;
		for (McpServerRuntime runtime : runtimes) {
			McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime,
					Schedulers.fromExecutorService(mcpThreadPool));
			RouterFunction<ServerResponse> runtimeRouter = handler.router();
			router = router == null ? runtimeRouter : router.and(runtimeRouter);
		}

		// 无 runtime 时返回空路由，交回 WebFlux 默认 404 语义。
		return router == null ? RouterFunctions.route(org.springframework.web.reactive.function.server
				.RequestPredicates.all(), request -> reactor.core.publisher.Mono.empty()) : router;
	}
}
