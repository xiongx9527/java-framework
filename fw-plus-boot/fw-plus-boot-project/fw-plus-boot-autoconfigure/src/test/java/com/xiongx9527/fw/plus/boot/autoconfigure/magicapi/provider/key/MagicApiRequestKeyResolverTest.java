package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.key;

import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.model.ApiInfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagicApiRequestKeyResolverTest {

	private final MagicApiRequestKeyResolver resolver = new MagicApiRequestKeyResolver();

	@Test
	void literalKeyIsReturnedAsConfigured() {
		RequestEntity entity = entity(api("api-1", "/orders", "POST"),
				params("tenant", "t1"), Collections.emptyMap(), body("bizNo", "B001"));

		String key = resolver.resolve(entity.getApiInfo(), entity, "magicapi:hot:data");

		assertThat(key).isEqualTo("magicapi:hot:data");
	}

	@Test
	void spelExpressionCanUseRequestContextVariables() {
		RequestEntity entity = entity(api("api-2", "/pay/{id}", "POST"),
				params("tenant", "t1"), Collections.singletonMap("X-Request-ID", "rid-1"), body("bizNo", "B001"));
		entity.pathVariables(Collections.singletonMap("id", "42"));

		String key = resolver.resolve(entity.getApiInfo(), entity,
				"#{#headers['X-Request-ID'] + ':' + #params['tenant'] + ':' + #pathVariables['id'] + ':' + #body['bizNo'] + ':' + #api.method}");

		assertThat(key).isEqualTo("rid-1:t1:42:B001:POST");
	}

	@Test
	void emptyConfiguredKeyUsesFallbackFingerprint() {
		RequestEntity entity = entity(api("api-3", "/pay", "POST"),
				params("tenant", "t1"), Collections.emptyMap(), body("bizNo", "B001"));

		String first = resolver.resolve(entity.getApiInfo(), entity, "");
		String second = resolver.resolve(entity.getApiInfo(), entity, null);

		assertThat(first).startsWith("sha256:");
		assertThat(second).isEqualTo(first);
	}

	@Test
	void differentRequestContextsGenerateDifferentFallbackFingerprints() {
		RequestEntity left = entity(api("api-4", "/pay", "POST"),
				params("tenant", "t1"), Collections.emptyMap(), body("bizNo", "B001"));
		RequestEntity right = entity(api("api-4", "/pay", "POST"),
				params("tenant", "t2"), Collections.emptyMap(), body("bizNo", "B002"));

		assertThat(resolver.fallbackFingerprint(left.getApiInfo(), left))
				.isNotEqualTo(resolver.fallbackFingerprint(right.getApiInfo(), right));
	}

	@Test
	void longResolvedKeyIsNormalizedToSha256Segment() {
		RequestEntity entity = entity(api("api-5", "/long", "GET"),
				Collections.emptyMap(), Collections.emptyMap(), null);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 140; i++) {
			builder.append('a');
		}

		String key = resolver.resolve(entity.getApiInfo(), entity, builder.toString());

		assertThat(key).startsWith("sha256:");
		assertThat(key.length()).isLessThan(builder.length());
	}

	@Test
	void invalidSpelThrowsClearException() {
		RequestEntity entity = entity(api("api-6", "/bad", "GET"),
				Collections.emptyMap(), Collections.emptyMap(), null);

		assertThatThrownBy(() -> resolver.resolve(entity.getApiInfo(), entity, "#{#missing.nope}"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Magic-API Key SpEL 解析失败");
	}

	private ApiInfo api(String id, String path, String method) {
		ApiInfo info = new ApiInfo();
		info.setId(id);
		info.setPath(path);
		info.setMethod(method);
		return info;
	}

	private RequestEntity entity(ApiInfo info, Map<String, Object> params, Map<String, Object> headers, Object body) {
		return RequestEntity.create()
				.info(info)
				.parameters(params)
				.pathVariables(Collections.emptyMap())
				.setHeaders(headers)
				.setRequestBody(body);
	}

	private Map<String, Object> params(String key, Object value) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(key, value);
		return map;
	}

	private Map<String, Object> body(String key, Object value) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put(key, value);
		return map;
	}
}
