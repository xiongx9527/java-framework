package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.auth;

import cn.dev33.satoken.SaManager;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 知识库对外 API 的认证授权校验（设计 §3.3）。
 *
 * <p>provider 由 {@code kb_setting} 设置页选择，通过 {@link InstanceAuthRegistry} 解析内置认证服务：
 * provider 清单固定（{@code permit-all}、{@code sa-token-login}、{@code sa-token-oauth2}、
 * {@code sa-token-jwt}、{@code sa-token-apikey}、{@code sa-token-sso}），本类只做校验，不实现登录、
 * 令牌签发、凭据管理或 Sa-Token 服务端；不提供带页面操作的认证入口。</p>
 *
 * <p>留空按 {@code sa-token-login} 处理，保持改造前行为；provider 模块缺失或取值未知时 fail-closed
 * （抛 {@link KbUnauthorizedException}，HTTP 层转 error_code 1002），不回退。</p>
 *
 * <p>向后兼容既有凭据读取：{@code Authorization: Bearer}、明文 {@code Authorization}、{@code kb-token}
 * 仍可命中 {@code sa-token-login}；优先从 {@code Authorization} 提取（去 Bearer 前缀），
 * 其次 {@code kb-token}，并归一化到 Sa-Token tokenName 头供登录态校验读取。</p>
 */
public class KbSaTokenAuthChecker {

	/** 知识库对外 API 在认证注册表中的实例标识，仅用于日志与错误信息。 */
	public static final String INSTANCE_KEY = "knowledge-openapi";

	/** 认证失败异常：携带 Dify 兼容错误码 1002（设计 §3.4）。 */
	public static class KbUnauthorizedException extends RuntimeException {

		public KbUnauthorizedException(String message) {
			super(message);
		}
	}

	private final InstanceAuthRegistry registry;

	private final KbSettingService settingService;

	public KbSaTokenAuthChecker() {
		this(new InstanceAuthRegistry(), null);
	}

	public KbSaTokenAuthChecker(InstanceAuthRegistry registry, KbSettingService settingService) {
		this.registry = registry != null ? registry : new InstanceAuthRegistry();
		this.settingService = settingService;
	}

	/**
	 * 兼容入口：只携带 {@code Authorization} 与 {@code kb-token} 两个凭据来源。
	 *
	 * @param authorizationHeader 请求 Authorization 头（Bearer/明文 token）
	 * @param kbTokenHeader       可选的独立凭证头（kb-token）
	 * @return 校验通过的主体标识
	 */
	public Object check(String authorizationHeader, String kbTokenHeader) {
		Map<String, String> headers = new LinkedHashMap<>();
		if (StringUtils.hasText(authorizationHeader)) {
			headers.put("Authorization", authorizationHeader);
		}
		if (StringUtils.hasText(kbTokenHeader)) {
			headers.put("kb-token", kbTokenHeader);
		}
		return check(headers);
	}

	/**
	 * provider 化入口：依据设置页选择解析 provider，并用完整请求头做无界面凭据校验。
	 *
	 * @param headers 请求头集合（大小写不敏感的读取由 provider 内部完成）
	 * @return 校验通过的主体标识
	 * @throws KbUnauthorizedException 未配置 provider、provider 缺失/未知或凭据无效时
	 */
	public Object check(Map<String, String> headers) {
		String provider = resolveProvider();
		InstanceAuthProperties properties = new InstanceAuthProperties();
		properties.setProvider(provider);

		AuthService service;
		try {
			service = registry.resolve(INSTANCE_KEY, properties);
		} catch (IllegalArgumentException e) {
			// 未知 provider（含历史 custom）：fail-closed，不回退
			throw new KbUnauthorizedException("不支持的认证 provider: " + provider);
		}
		if (service == null) {
			throw new KbUnauthorizedException("未配置认证 provider");
		}

		AuthPrincipal principal = service.authenticate(new AuthRequest(
				INSTANCE_KEY, null, "openapi", null, null, normalizeCredentials(headers, provider)));
		if (principal == null || !principal.isAuthenticated()) {
			throw new KbUnauthorizedException("认证凭证无效或已过期");
		}
		return principal.getSubject();
	}

	private String resolveProvider() {
		if (settingService == null) {
			return KbSettingService.DEFAULT_OPEN_API_AUTH_PROVIDER;
		}
		String provider = settingService.openApiAuthProvider();
		return StringUtils.hasText(provider) ? provider : KbSettingService.DEFAULT_OPEN_API_AUTH_PROVIDER;
	}

	/**
	 * 保留既有凭据兼容：把 {@code Authorization}（去 Bearer 前缀）或 {@code kb-token} 归一化到
	 * Sa-Token tokenName 头，使 {@code sa-token-login} provider 能读到旧格式凭据；其余 provider
	 * 直接读取各自 header，不受影响。
	 */
	private Map<String, String> normalizeCredentials(Map<String, String> headers, String provider) {
		Map<String, String> normalized = new LinkedHashMap<>();
		if (headers != null) {
			normalized.putAll(headers);
		}
		if (!InstanceAuthRegistry.PROVIDER_SA_TOKEN_LOGIN.equals(provider)) {
			return normalized;
		}
		String tokenName = resolveTokenName();
		if (hasHeader(normalized, tokenName) || hasHeader(normalized, "satoken")) {
			return normalized;
		}
		String token = extractToken(header(normalized, "Authorization"));
		if (!StringUtils.hasText(token)) {
			token = header(normalized, "kb-token");
		}
		if (StringUtils.hasText(token)) {
			normalized.put(tokenName, token);
		}
		return normalized;
	}

	private String resolveTokenName() {
		try {
			String tokenName = SaManager.getConfig().getTokenName();
			return StringUtils.hasText(tokenName) ? tokenName : "satoken";
		} catch (Throwable ignored) {
			return "satoken";
		}
	}

	private boolean hasHeader(Map<String, String> headers, String name) {
		return StringUtils.hasText(header(headers, name));
	}

	private String header(Map<String, String> headers, String name) {
		if (headers == null || name == null) {
			return null;
		}
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if (entry.getKey() != null && name.equalsIgnoreCase(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	/** 从 Authorization 头提取 token：优先 Bearer 前缀，否则整值视为 token。 */
	private String extractToken(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader)) {
			return null;
		}
		String value = authorizationHeader.trim();
		if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return value.substring(7).trim();
		}
		return value;
	}
}