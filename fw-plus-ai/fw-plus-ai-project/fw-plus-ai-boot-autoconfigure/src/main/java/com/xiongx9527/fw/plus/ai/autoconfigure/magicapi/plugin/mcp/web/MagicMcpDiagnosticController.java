package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.web;

import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.feature.model.ServerStorePrompt;
import com.ajaxjs.mcp.server.feature.model.ServerStoreResource;
import com.ajaxjs.mcp.server.feature.model.ServerStoreTool;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpCapabilities;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpHeaderNames;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MCP 诊断接口：查看各实例的能力清单，并按 uri / 提示词名试运行
 * {@code resources/read} 与 {@code prompts/get}。
 *
 * <p>与 Boot 层同名控制器的语义对齐：只复用既有能力执行路径（store 里的 instance + method 反射），
 * 不新增绕过认证/会话/过滤的入口。</p>
 */
@Slf4j
public class MagicMcpDiagnosticController extends MagicController implements MagicExceptionHandler {

	private final Supplier<List<McpServerRuntime>> runtimeProvider;

	public MagicMcpDiagnosticController(MagicConfiguration configuration) {
		this(configuration, () -> SpringUtils.getBean(MagicMcpNotifier.class).all());
	}

	MagicMcpDiagnosticController(MagicConfiguration configuration, Supplier<List<McpServerRuntime>> runtimeProvider) {
		super(configuration);
		this.runtimeProvider = runtimeProvider;
	}

	/**
	 * 指定运行实例的能力清单。
	 */
	@RequestMapping("/mcp/diagnostics/capabilities")
	@ResponseBody
	public JsonBean<List<Map<String, Object>>> capabilities(@RequestParam("key") String key,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		McpServerRuntime runtime = resolveRuntime(key);
		if (runtime == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
		if (!authorize(runtime, request, response, null, null)) {
			return null;
		}
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("key", runtime.getKey());
		item.put("endpoint", runtime.getEndpoint());
		item.put("transport", runtime.getTransport());
		item.put("tools", toolNames(runtime.getFeatureMgr()));
		item.put("resources", resourceUris(runtime.getFeatureMgr()));
		item.put("prompts", promptNames(runtime.getFeatureMgr()));
		return new JsonBean<>(Collections.singletonList(item));
	}

	/**
	 * 试运行 {@code resources/read}：返回指定 uri 的资源内容。
	 */
	@RequestMapping("/mcp/diagnostics/execute")
	@ResponseBody
	public JsonBean<Object> execute(@RequestBody Map<String, String> body,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		String uri = body.get("uri");
		if (uri == null || uri.trim().isEmpty()) {
			return new JsonBean<>("uri 不能为空");
		}
		String key = body.get("key");

		McpServerRuntime runtime = resolveRuntime(key);
		if (runtime == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
		if (!authorize(runtime, request, response, body.toString(), "resources")) {
			return null;
		}
		FeatureMgr featureMgr = runtime.getFeatureMgr();
		ServerStoreResource resource = featureMgr.getResourceStore().get(uri);
		if (resource == null) {
			return new JsonBean<>("资源不存在: " + uri);
		}

		try {
			return new JsonBean<>(resource.getMethod().invoke(resource.getInstance()));
		} catch (Exception e) {
			log.warn("诊断执行 resources/read 失败，uri={}", uri, e);
			return new JsonBean<>("读取失败: " + e.getMessage());
		}
	}

	/**
	 * 试运行 {@code prompts/get}：返回指定提示词的消息。
	 */
	@RequestMapping("/mcp/diagnostics/executePrompt")
	@ResponseBody
	public JsonBean<Object> executePrompt(@RequestBody Map<String, String> body,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		String name = body.get("name");
		if (name == null || name.trim().isEmpty()) {
			return new JsonBean<>("name 不能为空");
		}

		McpServerRuntime runtime = resolveRuntime(body.get("key"));
		if (runtime == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
		if (!authorize(runtime, request, response, body.toString(), "prompts")) {
			return null;
		}
		FeatureMgr featureMgr = runtime.getFeatureMgr();
		ServerStorePrompt prompt = featureMgr.getPromptStore().get(name);
		if (prompt == null) {
			return new JsonBean<>("提示词不存在: " + name);
		}

		try {
			return new JsonBean<>(prompt.getMethod().invoke(prompt.getInstance()));
		} catch (Exception e) {
			log.warn("诊断执行 prompts/get 失败，name={}", name, e);
			return new JsonBean<>("读取失败: " + e.getMessage());
		}
	}

	private McpServerRuntime resolveRuntime(String key) {
		if (key == null || key.trim().isEmpty()) {
			return null;
		}
		for (McpServerRuntime runtime : runtimeProvider.get()) {
			if (key.equals(runtime.getKey())) {
				return runtime;
			}
		}
		return null;
	}

	private boolean authorize(McpServerRuntime runtime, HttpServletRequest request, HttpServletResponse response,
			String body, String capability) throws IOException {
		com.xiongx9527.fw.plus.auth.core.AuthService authService = runtime.getAuthService();
		McpPrincipal principal = authService == null ? McpPrincipal.ANONYMOUS
				: McpAuthService.adapt(authService).authenticate(new McpRequest(runtime.getKey(), null, runtime.getTransport(),
						runtime.getEndpoint(), body, requestHeaders(runtime, request)));
		if (principal == null || !principal.isAuthenticated()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		if (runtime.getProperties() != null && runtime.getProperties().isScopeCheckEnabled()
				&& (capability == null
						? !McpCapabilities.covers(principal, "tools")
								|| !McpCapabilities.covers(principal, "resources")
								|| !McpCapabilities.covers(principal, "prompts")
						: !McpCapabilities.covers(principal, capability))) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		return true;
	}

	private Map<String, String> requestHeaders(McpServerRuntime runtime, HttpServletRequest request) {
		Map<String, String> headers = new LinkedHashMap<>();
		Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, request.getHeader(name)));
		new McpHeaderNames(runtime.getProperties() == null ? null : runtime.getProperties().getHeaders())
				.applyCustomNames(headers);
		return headers;
	}

	private List<String> toolNames(FeatureMgr featureMgr) {
		List<String> names = new ArrayList<>(featureMgr.getToolStore().keySet());
		Collections.sort(names);
		return names;
	}

	private List<String> resourceUris(FeatureMgr featureMgr) {
		List<String> uris = new ArrayList<>(featureMgr.getResourceStore().keySet());
		Collections.sort(uris);
		return uris;
	}

	private List<String> promptNames(FeatureMgr featureMgr) {
		List<String> names = new ArrayList<>(featureMgr.getPromptStore().keySet());
		Collections.sort(names);
		return names;
	}
}
