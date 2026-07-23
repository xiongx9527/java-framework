package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ParamConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Option;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepeatSubmitKeyResolver {

	private static final String PREFIX = "magicapi:repeat:submit";
	private static final int MAX_SEGMENT_LENGTH = 128;

	private final ApplicationContext applicationContext;
	private final RepeatSubmitKeySuffixProvider defaultSuffixProvider;
	private final ExpressionParser expressionParser;

	public RepeatSubmitKeyResolver(ApplicationContext applicationContext,
								   RepeatSubmitKeySuffixProvider defaultSuffixProvider) {
		this.applicationContext = applicationContext;
		this.defaultSuffixProvider = defaultSuffixProvider;
		this.expressionParser = new SpelExpressionParser();
	}

	public String resolve(ApiInfo info, RequestEntity requestEntity) {
		String main = resolveMainKey(info, requestEntity);
		String suffix = resolveSuffix(info, requestEntity);
		List<String> parts = new ArrayList<>();
		parts.add(PREFIX);
		parts.add(normalizeSegment(interfaceKey(info)));
		parts.add(normalizeSegment(StringUtils.defaultIfBlank(main, fallbackFingerprint(info, requestEntity))));
		if (StringUtils.isNotBlank(suffix)) {
			parts.add(normalizeSegment(suffix));
		}
		return StringUtils.join(parts, ":");
	}

	private String resolveMainKey(ApiInfo info, RequestEntity requestEntity) {
		String configured = ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_KEY.getValue());
		if (StringUtils.isBlank(configured)) {
			return null;
		}
		String trimmed = configured.trim();
		if (isSpel(trimmed)) {
			return String.valueOf(evaluateSpel(trimmed, info, requestEntity));
		}
		return trimmed;
	}

	private String resolveSuffix(ApiInfo info, RequestEntity requestEntity) {
		String beanName = ParamConfig.get(info, Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue());
		if (beanName == null && !hasOption(info, Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue())) {
			beanName = Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getDefaultValue();
		}
		RepeatSubmitKeySuffixProvider provider = getSuffixProvider(beanName);
		return provider.getSuffix(requestEntity);
	}

	private RepeatSubmitKeySuffixProvider getSuffixProvider(String beanName) {
		if (StringUtils.isNotBlank(beanName) && applicationContext != null && applicationContext.containsBean(beanName)) {
			Object bean = applicationContext.getBean(beanName);
			if (bean instanceof RepeatSubmitKeySuffixProvider) {
				return (RepeatSubmitKeySuffixProvider) bean;
			}
		}
		return defaultSuffixProvider;
	}

	private boolean isSpel(String value) {
		return value.startsWith("#{#") && value.endsWith("}");
	}

	private boolean hasOption(ApiInfo info, String key) {
		if (info == null || info.getOptions() == null) {
			return false;
		}
		for (Option option : info.getOptions()) {
			if (key.equals(option.getName())) {
				return true;
			}
		}
		return false;
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
			throw new IllegalArgumentException("重复提交 Key SpEL 解析失败: " + configured, e);
		}
	}

	private String interfaceKey(ApiInfo info) {
		if (info == null) {
			return "unknown";
		}
		if (StringUtils.isNotBlank(info.getId())) {
			return info.getId();
		}
		return StringUtils.defaultIfBlank(info.getMethod(), "UNKNOWN") + ":" + StringUtils.defaultIfBlank(info.getPath(), "unknown");
	}

	private String fallbackFingerprint(ApiInfo info, RequestEntity requestEntity) {
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

	private String normalizeSegment(String value) {
		String segment = StringUtils.defaultString(value).trim();
		if (segment.length() <= MAX_SEGMENT_LENGTH) {
			return segment;
		}
		return digest(segment);
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
