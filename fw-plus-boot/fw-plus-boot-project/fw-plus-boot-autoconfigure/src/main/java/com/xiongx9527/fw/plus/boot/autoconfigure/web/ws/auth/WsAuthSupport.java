package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsInstanceResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig.WsInstance;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebSocket 握手认证：按请求路径解析命中的实例，再用该实例的 provider 走
 * {@link InstanceAuthRegistry} 校验凭据。
 *
 * <p>webmvc 与 webflux 两条栈共用本类，认证口径一致：只校验请求携带的凭据，不实现登录、
 * 令牌签发或凭据管理。provider 与 {@code appId} 相反，不跨实例继承：命名实例只认
 * {@code servers[].auth}，未配置即拒绝；默认实例只认顶层 {@code fw.plus.ws.auth}。</p>
 *
 * <p>凭据来源为握手 query 参数 + 请求头，两者合并为同一份凭据映射（同名时请求头优先），
 * 由 provider 的读取器按名解析（{@code Authorization: Bearer}、tokenName（默认
 * {@code satoken}）、{@code apikey}、{@code ticket} 等）。</p>
 *
 * <p>未配置 provider、provider 模块缺失或凭据校验失败时一律拒绝（fail-closed）；未知 provider
 * 由 {@link InstanceAuthRegistry#resolve(String, InstanceAuthProperties)} 抛
 * {@link IllegalArgumentException}，与 MCP 语义一致。</p>
 *
 * @author zzy
 */
public class WsAuthSupport {

	/**
	 * 默认实例在 registry 内的标识：默认实例没有 key。
	 */
	public static final String DEFAULT_INSTANCE_KEY = "default";

	private static final String TRANSPORT = "websocket";

	private final WsConfig wsConfig;

	private final InstanceAuthRegistry registry;

	public WsAuthSupport(WsConfig wsConfig, InstanceAuthRegistry registry) {
		this.wsConfig = wsConfig;
		this.registry = registry;
	}

	/**
	 * 校验一次握手请求的凭据。
	 *
	 * @param requestPath  握手请求路径（{@code URI#getPath()}）
	 * @param credentials  合并后的凭据映射（query 参数 + 请求头，同名时请求头优先）
	 * @return 认证通过返回 {@code true}；未配置 provider、模块缺失或校验失败返回 {@code false}
	 * @throws IllegalArgumentException provider 不受支持时
	 */
	public boolean isAuthenticated(String requestPath, Map<String, String> credentials) {
		InstanceAuthProperties auth = resolveAuth(requestPath);
		String instanceKey = resolveInstanceKey(requestPath);
		AuthService service = registry == null ? null : registry.resolve(instanceKey, auth);
		if (service == null) {
			return false;
		}
		AuthPrincipal principal = service.authenticate(new AuthRequest(
				instanceKey, null, TRANSPORT, requestPath, null, credentials));
		return principal != null && principal.isAuthenticated();
	}

	/**
	 * 启动期校验全部实例的 provider：只校验「是否受支持」，模块缺失留给握手期 fail-closed，
	 * 未知 provider（含历史 {@code custom}）在此抛 {@link IllegalArgumentException}。
	 *
	 * @throws IllegalArgumentException 任一实例 provider 不受支持时
	 */
	public static void validateProviders(WsConfig config, InstanceAuthRegistry registry) {
		if (config == null || registry == null) {
			return;
		}
		validateProvider(registry, "fw.plus.ws.auth.provider", config.getAuth());
		if (config.getServers() == null) {
			return;
		}
		for (int i = 0; i < config.getServers().size(); i++) {
			WsInstance server = config.getServers().get(i);
			if (server != null) {
				validateProvider(registry, "fw.plus.ws.servers[" + i + "].auth.provider", server.getAuth());
			}
		}
	}

	private static void validateProvider(InstanceAuthRegistry registry, String keyPath,
			InstanceAuthProperties auth) {
		if (auth == null || StringUtils.isBlank(auth.getProvider())) {
			return;
		}
		// 未知 provider 直接抛出；已知但模块缺失返回 RejectingAuthService，不在启动期失败。
		registry.resolve(keyPath, auth);
	}

	/**
	 * 合并握手凭据：先取 query 参数，再叠加请求头（同名时请求头优先）。
	 *
	 * @param rawQuery    原始 query 串（{@code URI#getQuery()}），可为 {@code null}
	 * @param headerValues 请求头键值（每个 header 取首个值），可为 {@code null}
	 * @return 保序的凭据映射
	 */
	public static Map<String, String> mergeCredentials(String rawQuery, Map<String, String> headerValues) {
		Map<String, String> credentials = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(rawQuery)) {
			for (String pair : rawQuery.split("&")) {
				if (pair.isEmpty()) {
					continue;
				}
				int separator = pair.indexOf('=');
				String name = separator >= 0 ? pair.substring(0, separator) : pair;
				String value = separator >= 0 ? pair.substring(separator + 1) : "";
				name = urlDecode(name);
				if (StringUtils.isNotBlank(name)) {
					credentials.put(name, urlDecode(value));
				}
			}
		}
		if (headerValues != null) {
			headerValues.forEach((name, value) -> {
				if (StringUtils.isNotBlank(name) && value != null) {
					credentials.put(name, value);
				}
			});
		}
		return credentials;
	}

	private static String urlDecode(String value) {
		if (value == null) {
			return null;
		}
		try {
			return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException | IllegalArgumentException e) {
			return value;
		}
	}

	private InstanceAuthProperties resolveAuth(String requestPath) {
		WsInstance instance = WsInstanceResolver.resolve(wsConfig, requestPath);
		if (instance != null) {
			return instance.getAuth();
		}
		return wsConfig == null ? null : wsConfig.getAuth();
	}

	private String resolveInstanceKey(String requestPath) {
		WsInstance instance = WsInstanceResolver.resolve(wsConfig, requestPath);
		if (instance != null && StringUtils.isNotBlank(instance.getKey())) {
			return instance.getKey().trim();
		}
		return DEFAULT_INSTANCE_KEY;
	}
}