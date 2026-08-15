package com.xiongx9527.fw.plus.tinyengine.support.ai;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * OpenAI 兼容接口根地址规范化工具。
 */
public final class OpenAiCompatibleBaseUrl {

	private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

	private OpenAiCompatibleBaseUrl() {
	}

	public static String normalize(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("AI model baseUrl is required");
		}
		String trimmed = value.trim();
		URI uri;
		try {
			uri = new URI(trimmed);
		} catch (URISyntaxException ex) {
			throw new IllegalArgumentException("AI model baseUrl must be a valid HTTP URL");
		}
		String scheme = uri.getScheme();
		if (scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
				|| uri.getHost() == null || uri.getRawUserInfo() != null) {
			throw new IllegalArgumentException("AI model baseUrl must be a valid HTTP URL");
		}
		if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
			throw new IllegalArgumentException("AI model baseUrl must not contain query parameters or fragments");
		}

		String path = uri.getRawPath();
		path = path == null ? "" : trimTrailingSlashes(path);
		if (path.toLowerCase(Locale.ENGLISH).endsWith(CHAT_COMPLETIONS_PATH)) {
			path = trimTrailingSlashes(path.substring(0, path.length() - CHAT_COMPLETIONS_PATH.length()));
		}
		if (path.length() == 0) {
			path = "/v1";
		} else if (path.charAt(0) != '/') {
			path = "/" + path;
		}
		return scheme.toLowerCase(Locale.ENGLISH) + "://" + uri.getRawAuthority() + path;
	}

	private static String trimTrailingSlashes(String value) {
		int end = value.length();
		while (end > 0 && value.charAt(end - 1) == '/') {
			end--;
		}
		return value.substring(0, end);
	}
}
