package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options.MagicS3PlatformOption;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options.MagicS3PlatformOptionProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.ssssssss.magicapi.core.annotation.Valid;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicS3ControllerPlatformOptionsTest {

	@Test
	void platformOptionsEndpointRequiresAuthenticatedConsoleAccess() throws Exception {
		Method method = MagicS3Controller.class.getMethod("platformOptions", MagicHttpServletRequest.class);

		GetMapping getMapping = method.getAnnotation(GetMapping.class);
		assertNotNull(getMapping);
		assertEquals("/s3/datasource/platform/options", getMapping.value()[0]);

		Valid valid = method.getAnnotation(Valid.class);
		assertNotNull(valid);
		assertTrue(valid.requireLogin());

		assertEquals(JsonBean.class, method.getReturnType());
		ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
		ParameterizedType dataType = (ParameterizedType) returnType.getActualTypeArguments()[0];
		assertEquals(List.class, dataType.getRawType());
		assertEquals(MagicS3PlatformOption.class, dataType.getActualTypeArguments()[0]);
	}

	@Test
	void platformOptionsRejectsRequestsWithoutViewPermission() {
		MagicConfiguration configuration = new MagicConfiguration();
		configuration.setAuthorizationInterceptor(new AuthorizationInterceptor() {
			@Override
			public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization) {
				return false;
			}
		});

		MagicS3Controller controller = new MagicS3Controller(configuration, mock(MagicS3PlatformOptionProvider.class));

		Assertions.assertThrows(RuntimeException.class, () -> controller.platformOptions(mock(MagicHttpServletRequest.class)));
	}

	@Test
	void platformOptionsReturnsProviderData() {
		MagicConfiguration configuration = new MagicConfiguration();
		configuration.setAuthorizationInterceptor(new AuthorizationInterceptor() {
			@Override
			public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization) {
				return true;
			}
		});
		MagicS3PlatformOptionProvider provider = mock(MagicS3PlatformOptionProvider.class);
		List<MagicS3PlatformOption> providerOptions = Collections.singletonList(new MagicS3PlatformOption("minio", "minio"));
		when(provider.getOptions()).thenReturn(providerOptions);
		MagicS3Controller controller = new MagicS3Controller(configuration, provider);

		JsonBean<List<MagicS3PlatformOption>> response = controller.platformOptions(mock(MagicHttpServletRequest.class));

		assertNotNull(response);
		assertSame(providerOptions, response.get("data"));
	}
}
