package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.key;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.model.ApiInfo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MagicApiRequestKeyResolver {

	private static final int MAX_SEGMENT_LENGTH = 128;

	private final ExpressionParser expressionParser;

	public MagicApiRequestKeyResolver() {
		this.expressionParser = new SpelExpressionParser();
	}

	public String resolve(ApiInfo info, RequestEntity requestEntity, String configuredKey) {
		String key = resolveConfiguredKey(info, requestEntity, configuredKey);
		return normalizeSegment(StringUtils.defaultIfBlank(key, fallbackFingerprint(info, requestEntity)));
	}

	public String fallbackFingerprint(ApiInfo info, RequestEntity requestEntity) {
		Map<String, Object> value = new LinkedHashMap<>();
		if (info != null) {
			value.put("id", info.getId());
			value.put("path", info.getPath());
			value.put("method", info.getMethod());
		}
		if (requestEntity != null) {
			value.put("headers", requestEntity.getHeaders());
			value.put("params", requestEntity.getParameters());
			value.put("pathVariables", requestEntity.getPathVariables());
			value.put("body", requestEntity.getRequestBody());
		}
		return digest(value);
	}

	public String interfaceKey(ApiInfo info) {
		if (info == null) {
			return "unknown";
		}
		if (StringUtils.isNotBlank(info.getId())) {
			return info.getId();
		}
		return StringUtils.defaultIfBlank(info.getMethod(), "UNKNOWN") + ":" + StringUtils.defaultIfBlank(info.getPath(), "unknown");
	}

	public String normalizeSegment(String value) {
		String segment = StringUtils.defaultString(value).trim();
		if (segment.length() <= MAX_SEGMENT_LENGTH) {
			return segment;
		}
		return digest(segment);
	}

	private String resolveConfiguredKey(ApiInfo info, RequestEntity requestEntity, String configuredKey) {
		if (StringUtils.isBlank(configuredKey)) {
			return null;
		}
		String trimmed = configuredKey.trim();
		if (isSpel(trimmed)) {
			Object value = evaluateSpel(trimmed, info, requestEntity);
			return value == null ? null : String.valueOf(value);
		}
		return trimmed;
	}

	private boolean isSpel(String value) {
		return value.startsWith("#{#") && value.endsWith("}");
	}

	private Object evaluateSpel(String configured, ApiInfo info, RequestEntity requestEntity) {
		try {
			String expression = configured.substring(2, configured.length() - 1);
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setVariable("request", requestEntity == null ? null : requestEntity.getRequest());
			context.setVariable("response", requestEntity == null ? null : requestEntity.getResponse());
			context.setVariable("headers", requestEntity == null ? null : requestEntity.getHeaders());
			context.setVariable("params", requestEntity == null ? null : requestEntity.getParameters());
			context.setVariable("pathVariables", requestEntity == null ? null : requestEntity.getPathVariables());
			context.setVariable("body", requestEntity == null ? null : requestEntity.getRequestBody());
			context.setVariable("api", info);
			context.setVariable("requestEntity", requestEntity);
			return expressionParser.parseExpression(expression).getValue(context);
		} catch (Exception e) {
			throw new IllegalArgumentException("Magic-API Key SpEL 解析失败: " + configured, e);
		}
	}

	private String digest(Object value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = JSON.toJSONString(value, JSONWriter.Feature.MapSortField).getBytes(StandardCharsets.UTF_8);
			byte[] encoded = digest.digest(bytes);
			StringBuilder builder = new StringBuilder();
			for (byte b : encoded) {
				builder.append(String.format("%02x", b));
			}
			return "sha256:" + builder.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm is not available", e);
		}
	}
}
