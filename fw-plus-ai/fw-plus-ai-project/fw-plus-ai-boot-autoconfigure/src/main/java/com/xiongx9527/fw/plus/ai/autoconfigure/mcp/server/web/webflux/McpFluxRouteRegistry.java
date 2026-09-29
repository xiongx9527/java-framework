package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webflux;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer.RuntimeRouteBinder;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * WebFlux 路由注册表：支持运行期增删 runtime 路由。
 *
 * <p>WebFlux 在启动期把所有 {@link RouterFunction} bean 聚合成 composite，之后不再重读 bean；
 * 因此对外暴露一个不变的「委托路由」，内部持有一个 {@code volatile} 的实际组合，
 * {@link #bind}/{@link #unbind} 时重建该组合，运行期请求即可命中最新路由。</p>
 */
public class McpFluxRouteRegistry implements RuntimeRouteBinder {

	private final Map<String, RouterFunction<ServerResponse>> routers = new LinkedHashMap<>();

	private final Scheduler scheduler;

	private volatile RouterFunction<ServerResponse> delegate;

	public McpFluxRouteRegistry(List<McpServerRuntime> runtimes, ExecutorService mcpThreadPool) {
		this.scheduler = reactor.core.scheduler.Schedulers.fromExecutorService(mcpThreadPool);
		if (runtimes != null) {
			for (McpServerRuntime runtime : runtimes) {
				put(runtime);
			}
		}
		rebuild();
	}

	/**
	 * 供 WebFlux 装配的不变入口：每次请求都从当前 delegate 取路由。
	 *
	 * @return 路由函数
	 */
	public RouterFunction<ServerResponse> router() {
		return new RouterFunction<ServerResponse>() {
			@Override
			public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
				RouterFunction<ServerResponse> current = delegate;
				return current == null ? Mono.empty() : current.route(request);
			}

			@Override
			public void accept(RouterFunctions.Visitor visitor) {
				RouterFunction<ServerResponse> current = delegate;
				if (current != null) {
					current.accept(visitor);
				}
			}
		};
	}

	@Override
	public synchronized void bind(McpServerRuntime runtime) {
		put(runtime);
		rebuild();
	}

	@Override
	public synchronized void unbind(McpServerRuntime runtime) {
		routers.remove(runtime.getKey());
		rebuild();
	}

	private void put(McpServerRuntime runtime) {
		if (runtime == null || runtime.getKey() == null) {
			return;
		}
		McpFluxTransportHandler handler = new McpFluxTransportHandler(runtime, scheduler);
		routers.put(runtime.getKey(), handler.router());
	}

	private void rebuild() {
		RouterFunction<ServerResponse> combined = null;
		for (RouterFunction<ServerResponse> router : routers.values()) {
			combined = combined == null ? router : combined.and(router);
		}
		delegate = combined;
	}
}
