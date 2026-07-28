package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Option;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpSession;

import java.security.Principal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepeatSubmitKeyResolverTest {

	private StaticApplicationContext applicationContext;
	private RepeatSubmitKeyResolver resolver;

	@AfterEach
	void closeContext() {
		if (applicationContext != null) {
			applicationContext.close();
		}
	}

	@Test
	void defaultLiteralKeyDoesNotInvokeSpelAndBuildsStableKey() {
		initResolver();
		RequestEntity entity = entity(api("api-1", "/orders", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), "#{magicapi:repeat:submit}"),
				params("orderNo", "A001"), Collections.singletonMap("channel", "web"), body("amount", 100));

		String first = resolver.resolve(entity.getApiInfo(), entity);
		String second = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(first).isEqualTo(second);
		assertThat(first).startsWith("magicapi:repeat:submit:");
		assertThat(first).contains("api-1");
		assertThat(first).contains("default-suffix");
	}

	@Test
	void spelExpressionCanUseHeadersParamsPathVariablesBodyAndApi() {
		initResolver();
		RequestEntity entity = entity(api("api-2", "/pay/{id}", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY.getValue(),
				"#{#headers['X-Request-ID'] + ':' + #params['tenant'] + ':' + #pathVariables['id'] + ':' + #body['bizNo'] + ':' + #api.method}"),
				params("tenant", "t1"), Collections.singletonMap("X-Request-ID", "rid-1"), body("bizNo", "B001"));
		entity.pathVariables(Collections.singletonMap("id", "42"));

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).contains("rid-1:t1:42:B001:POST");
	}

	@Test
	void emptyKeyFallsBackToRequestFingerprint() {
		initResolver();
		RequestEntity entity = entity(api("api-3", "/pay", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), ""),
				params("tenant", "t1"), Collections.emptyMap(), body("bizNo", "B001"));

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).startsWith("magicapi:repeat:submit:");
		assertThat(key).contains("api-3");
		assertThat(key).contains("sha256:");
	}

	@Test
	void missingKeyFallsBackToRequestFingerprint() {
		initResolver();
		RequestEntity entity = entity(api("api-3-missing", "/pay", "POST"),
				params("tenant", "t1"), Collections.emptyMap(), body("bizNo", "B001"));

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).startsWith("magicapi:repeat:submit:");
		assertThat(key).contains("api-3-missing");
		assertThat(key).contains("sha256:");
	}

	@Test
	void differentRequestContextsGenerateDifferentKeys() {
		initResolver();
		RequestEntity left = entity(api("api-4", "/pay", "POST", Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), ""),
				params("tenant", "t1"), Collections.emptyMap(), body("bizNo", "B001"));
		RequestEntity right = entity(api("api-4", "/pay", "POST", Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), ""),
				params("tenant", "t2"), Collections.emptyMap(), body("bizNo", "B002"));

		assertThat(resolver.resolve(left.getApiInfo(), left)).isNotEqualTo(resolver.resolve(right.getApiInfo(), right));
	}

	@Test
	void configuredSuffixBeanIsUsedWhenItImplementsProviderInterface() {
		applicationContext = new StaticApplicationContext();
		applicationContext.registerBean("tenantSuffix", RepeatSubmitKeySuffixProvider.class, () -> requestEntity -> "tenant-9527");
		applicationContext.refresh();
		new SpringUtils().setApplicationContext(applicationContext);
		resolver = new RepeatSubmitKeyResolver(applicationContext, requestEntity -> "default-suffix");
		RequestEntity entity = entity(api("api-5", "/orders", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue(), "tenantSuffix"),
				Collections.emptyMap(), Collections.emptyMap(), null);

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).contains("tenant-9527");
	}

	@Test
	void missingOrWrongSuffixBeanFallsBackToDefaultProvider() {
		applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("wrongSuffix", String.class);
		applicationContext.refresh();
		new SpringUtils().setApplicationContext(applicationContext);
		resolver = new RepeatSubmitKeyResolver(applicationContext, requestEntity -> "default-suffix");
		RequestEntity entity = entity(api("api-6", "/orders", "POST",
				Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue(), "wrongSuffix"),
				Collections.emptyMap(), Collections.emptyMap(), null);

		String key = resolver.resolve(entity.getApiInfo(), entity);

		assertThat(key).contains("default-suffix");
	}

	@Test
	void defaultSuffixPrefersPrincipalThenUserIdThenSessionThenIp() {
		DefaultRepeatSubmitKeySuffixProvider provider = new DefaultRepeatSubmitKeySuffixProvider();
		MagicHttpServletRequest request = mock(MagicHttpServletRequest.class);
		when(request.getUserPrincipal()).thenReturn((Principal) () -> "principal-user");

		assertThat(provider.getSuffix(RequestEntity.create().request(request))).isEqualTo("principal-user");

		when(request.getUserPrincipal()).thenReturn(null);
		when(request.getAttribute("userId")).thenReturn("user-9527");
		assertThat(provider.getSuffix(RequestEntity.create().request(request))).isEqualTo("user-9527");

		when(request.getAttribute("userId")).thenReturn(null);
		MagicHttpSession session = mock(MagicHttpSession.class);
		Object nativeSession = new Object();
		when(session.getSession()).thenReturn(nativeSession);
		when(request.getSession()).thenReturn(session);
		assertThat(provider.getSuffix(RequestEntity.create().request(request))).isEqualTo(String.valueOf(nativeSession.hashCode()));

		when(request.getSession()).thenReturn(null);
		when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
		assertThat(provider.getSuffix(RequestEntity.create().request(request))).isEqualTo("10.0.0.1");
	}

	private void initResolver() {
		applicationContext = new StaticApplicationContext();
		applicationContext.refresh();
		new SpringUtils().setApplicationContext(applicationContext);
		resolver = new RepeatSubmitKeyResolver(applicationContext, requestEntity -> "default-suffix");
	}

	private ApiInfo api(String id, String path, String method, String optionName, String optionValue) {
		ApiInfo info = new ApiInfo();
		info.setId(id);
		info.setPath(path);
		info.setMethod(method);
		info.setOption(Collections.singletonList(new Option(optionName, optionValue)));
		return info;
	}

	private ApiInfo api(String id, String path, String method) {
		ApiInfo info = new ApiInfo();
		info.setId(id);
		info.setPath(path);
		info.setMethod(method);
		info.setOption(Collections.emptyList());
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
