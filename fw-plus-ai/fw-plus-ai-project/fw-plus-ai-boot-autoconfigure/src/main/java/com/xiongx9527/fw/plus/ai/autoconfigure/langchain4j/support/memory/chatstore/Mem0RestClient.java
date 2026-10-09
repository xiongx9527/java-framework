package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * mem0 兼容 REST 客户端：JDK {@link HttpURLConnection} 实现，零第三方 HTTP 依赖。
 *
 * <p>端点契约（相对 {@code base-url}，经真实自托管 mem0 实测校准）：</p>
 * <ul>
 *   <li>{@code GET /memories} — 按 {@code user_id} / {@code run_id} 过滤读取（{@code top_k} 截断）；</li>
 *   <li>{@code POST /memories} — 新增记忆（本存储以 {@code infer=false} 原文直存）；</li>
 *   <li>{@code PUT /memories/{id}} — 按 ID 更新记忆文本；</li>
 *   <li>{@code DELETE /memories/{id}} — 按 ID 删除记忆。</li>
 * </ul>
 * <p>路径<strong>不带</strong> {@code /api/v1/} 前缀与尾斜杠（带尾斜杠实测 404）；
 * 认证用 {@code X-API-Key} 头（实测 Bearer/双头均 401）。</p>
 *
 * <p>鉴权：配置了 {@code api-key} 时携带 {@code Authorization: Bearer} 头，留空则不发送。
 * 非 2xx 响应统一抛 {@link IllegalStateException}（携带状态码与响应体片段）。</p>
 */
class Mem0RestClient {

	private static final int MAX_ERROR_BODY = 512;

	private final String baseUrl;

	private final String apiKey;

	private final int connectTimeoutMillis;

	private final int readTimeoutMillis;

	Mem0RestClient(String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
		if (baseUrl == null || baseUrl.trim().isEmpty()) {
			throw new IllegalArgumentException("Mem0ChatMemoryStore: base-url 不能为空");
		}
		String trimmed = baseUrl.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("Mem0ChatMemoryStore: base-url 不能为空");
		}
		this.baseUrl = trimmed;
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.connectTimeoutMillis = toMillis(connectTimeout, 5000);
		this.readTimeoutMillis = toMillis(readTimeout, 10000);
	}

	/** GET 请求，{@code query} 为已编码的查询串（可为 {@code null}），返回响应体文本。 */
	String get(String path, String query) {
		return execute("GET", path, query, null);
	}

	/** POST 请求，{@code body} 为 JSON 请求体，返回响应体文本。 */
	String post(String path, String body) {
		return execute("POST", path, null, body);
	}

	/** PUT 请求，{@code body} 为 JSON 请求体，返回响应体文本。 */
	String put(String path, String body) {
		return execute("PUT", path, null, body);
	}

	/** DELETE 请求，返回响应体文本。 */
	String delete(String path) {
		return execute("DELETE", path, null, null);
	}

	private String execute(String method, String path, String query, String body) {
		String url = baseUrl + path + (query == null || query.isEmpty() ? "" : "?" + query);
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod(method);
			connection.setConnectTimeout(connectTimeoutMillis);
			connection.setReadTimeout(readTimeoutMillis);
			connection.setRequestProperty("Accept", "application/json");
			if (!apiKey.isEmpty()) {
				// 实测自托管 mem0：仅认 X-API-Key；Bearer 走 JWT 体系、双头同发均 401
				connection.setRequestProperty("X-API-Key", apiKey);
			}
			if (body != null) {
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
				byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
				connection.setFixedLengthStreamingMode(bytes.length);
				OutputStream out = connection.getOutputStream();
				try {
					out.write(bytes);
				} finally {
					out.close();
				}
			}
			int status = connection.getResponseCode();
			InputStream stream = status >= 200 && status < 300
					? connection.getInputStream() : connection.getErrorStream();
			String response = stream == null ? "" : readFully(stream);
			if (status < 200 || status >= 300) {
				throw new IllegalStateException(String.format(
						"mem0 请求失败: %s %s -> HTTP %d: %s", method, url, status, truncate(response)));
			}
			return response;
		} catch (IllegalStateException e) {
			throw e;
		} catch (IOException e) {
			throw new IllegalStateException(String.format("mem0 请求失败: %s %s -> %s", method, url, e.getMessage()), e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/** 拼接查询串（值按 UTF-8 URL 编码）。 */
	static String query(Map<String, String> params) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append('&');
			}
			builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
		}
		return builder.toString();
	}

	private static String encode(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (IOException e) {
			// URLEncoder.encode(String, String) 声明 UnsupportedEncodingException（UTF-8 恒存在）
			throw new IllegalStateException("UTF-8 编码不可用", e);
		}
	}

	private static String readFully(InputStream stream) {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		try {
			char[] buffer = new char[2048];
			int read;
			while ((read = reader.read(buffer)) >= 0) {
				builder.append(buffer, 0, read);
			}
		} catch (IOException e) {
			throw new IllegalStateException("读取 mem0 响应失败: " + e.getMessage(), e);
		} finally {
			try {
				reader.close();
			} catch (IOException ignore) {
				// 关闭失败不影响结果
			}
		}
		return builder.toString();
	}

	private static String truncate(String body) {
		if (body == null) {
			return "";
		}
		String flattened = body.replaceAll("\\s+", " ").trim();
		return flattened.length() <= MAX_ERROR_BODY ? flattened : flattened.substring(0, MAX_ERROR_BODY) + "...";
	}

	private static int toMillis(Duration duration, int fallback) {
		if (duration == null || duration.isNegative()) {
			return fallback;
		}
		long millis = duration.toMillis();
		return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
	}
}
