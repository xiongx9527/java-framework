package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.auth;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.util.StringUtils;

/**
 * 对外 API 的 Sa-Token 认证授权校验（设计 §3.3）：从 {@code Authorization: Bearer <token>}
 * 或任意 {@code Authorization}/{@code kb-token} 头中提取 Sa-Token token 并校验有效性。
 * 凭证/客户端管理复用现有 magicapi satoken 插件资源，本类只做校验。
 */
public class KbSaTokenAuthChecker {

	/** 认证失败异常：携带 Dify 兼容错误码 1002（设计 §3.4）。 */
	public static class KbUnauthorizedException extends RuntimeException {

		public KbUnauthorizedException(String message) {
			super(message);
		}
	}

	/**
	 * 校验请求凭证；未通过时抛出 {@link KbUnauthorizedException}（HTTP 层转换为 error_code 1002）。
	 *
	 * @param authorizationHeader 请求 Authorization 头（Bearer/明文 token）
	 * @param kbTokenHeader       可选的独立凭证头（kb-token）
	 * @return 校验通过的 loginId
	 */
	public Object check(String authorizationHeader, String kbTokenHeader) {
		String token = extractToken(authorizationHeader);
		if (!StringUtils.hasText(token)) {
			token = kbTokenHeader;
		}
		if (!StringUtils.hasText(token)) {
			throw new KbUnauthorizedException("缺少认证凭证：请在 Authorization 头中以 Bearer 方式携带 Sa-Token token");
		}
		Object loginId;
		try {
			loginId = StpUtil.getLoginIdByToken(token);
		} catch (Exception e) {
			throw new KbUnauthorizedException("认证凭证校验失败: " + e.getMessage());
		}
		if (loginId == null) {
			throw new KbUnauthorizedException("认证凭证无效或已过期");
		}
		return loginId;
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
