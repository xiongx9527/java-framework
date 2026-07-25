package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.MagicApiPluginAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserNamespace;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserObjectDetail;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserObjectInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserPreviewRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserPreviewResult;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserRoutineInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DbBrowserService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DbBrowserSqlBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service.DefaultDbBrowserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.ssssssss.magicapi.core.annotation.Valid;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicDbBrowserControllerTest {

	private static final String CONTROLLER_CLASS = "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.web.MagicDbBrowserController";
	private static final String CONFIGURATION_CLASS = "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.starter.MagicAPIDbBrowserConfiguration";

	@Test
	void endpointsRequireAuthenticatedConsoleAccessAndExposeTheDbBrowserContract() throws Exception {
		Class<?> controllerClass = Class.forName(CONTROLLER_CLASS);

		assertEndpoint(controllerClass, "listDataSources", GetMapping.class, "/db/browser/datasources", MagicHttpServletRequest.class);
		assertEndpoint(controllerClass, "listNamespaces", GetMapping.class, "/db/browser/namespaces", String.class, MagicHttpServletRequest.class);
		assertEndpoint(controllerClass, "listObjects", GetMapping.class, "/db/browser/objects", String.class, String.class, String.class, String.class, MagicHttpServletRequest.class);
		assertEndpoint(controllerClass, "listRoutines", GetMapping.class, "/db/browser/routines", String.class, String.class, String.class, String.class, MagicHttpServletRequest.class);
		assertEndpoint(controllerClass, "getObjectDetail", GetMapping.class, "/db/browser/object/detail", String.class, String.class, String.class, String.class, String.class, MagicHttpServletRequest.class);
		assertEndpoint(controllerClass, "preview", PostMapping.class, "/db/browser/preview", DbBrowserPreviewRequest.class, MagicHttpServletRequest.class);
	}

	@Test
	void endpointsDelegateToTheReadOnlyBrowserService() throws Exception {
		DbBrowserService service = mock(DbBrowserService.class);
		List<DbBrowserDataSourceInfo> dataSources = Collections.singletonList(new DbBrowserDataSourceInfo());
		List<DbBrowserNamespace> namespaces = Collections.singletonList(new DbBrowserNamespace());
		List<DbBrowserObjectInfo> objects = Collections.singletonList(new DbBrowserObjectInfo());
		List<DbBrowserRoutineInfo> routines = Collections.singletonList(new DbBrowserRoutineInfo());
		DbBrowserObjectDetail detail = new DbBrowserObjectDetail();
		DbBrowserPreviewResult preview = new DbBrowserPreviewResult();
		DbBrowserPreviewRequest previewRequest = new DbBrowserPreviewRequest();
		when(service.listDataSources()).thenReturn(dataSources);
		when(service.listNamespaces("db")).thenReturn(namespaces);
		when(service.listObjects("db", "catalog", "schema", "TABLE")).thenReturn(objects);
		when(service.listRoutines("db", "catalog", "schema", "PROCEDURE")).thenReturn(routines);
		when(service.getObjectDetail("db", "catalog", "schema", "orders", "TABLE")).thenReturn(detail);
		when(service.preview(previewRequest)).thenReturn(preview);

		Object controller = controller(service, true);
		MagicHttpServletRequest request = mock(MagicHttpServletRequest.class);

		assertSame(dataSources, data(invoke(controller, "listDataSources", request)));
		assertSame(namespaces, data(invoke(controller, "listNamespaces", "db", request)));
		assertSame(objects, data(invoke(controller, "listObjects", "db", "catalog", "schema", "TABLE", request)));
		assertSame(routines, data(invoke(controller, "listRoutines", "db", "catalog", "schema", "PROCEDURE", request)));
		assertSame(detail, data(invoke(controller, "getObjectDetail", "db", "catalog", "schema", "orders", "TABLE", request)));
		assertSame(preview, data(invoke(controller, "preview", previewRequest, request)));

		verify(service).listDataSources();
		verify(service).listNamespaces("db");
		verify(service).listObjects("db", "catalog", "schema", "TABLE");
		verify(service).listRoutines("db", "catalog", "schema", "PROCEDURE");
		verify(service).getObjectDetail("db", "catalog", "schema", "orders", "TABLE");
		verify(service).preview(previewRequest);
	}

	@Test
	void previewRequestJsonContractUsesDatasourceKey() throws Exception {
		DbBrowserPreviewRequest request = new ObjectMapper().readValue(
				"{\"datasourceKey\":\"orders\",\"objectName\":\"items\",\"objectType\":\"TABLE\"}",
				DbBrowserPreviewRequest.class);

		assertEquals("orders", request.getDatasourceKey());
		assertEquals("items", request.getObjectName());
	}

	@Test
	void endpointsRejectRequestsWithoutViewPermissionAndPropagateServiceValidationErrors() throws Exception {
		DbBrowserService deniedService = mock(DbBrowserService.class);
		Object deniedController = controller(deniedService, false);
		assertThrows(RuntimeException.class, () -> invoke(deniedController, "listDataSources", mock(MagicHttpServletRequest.class)));

		DbBrowserService validatingService = mock(DbBrowserService.class);
		when(validatingService.listNamespaces("missing")).thenThrow(new IllegalArgumentException("unknown datasource"));
		DbBrowserPreviewRequest request = new DbBrowserPreviewRequest();
		when(validatingService.preview(request)).thenThrow(new IllegalArgumentException("unknown column"));
		Object controller = controller(validatingService, true);
		assertThrows(IllegalArgumentException.class, () -> invoke(controller, "listNamespaces", "missing", mock(MagicHttpServletRequest.class)));
		assertThrows(IllegalArgumentException.class, () -> invoke(controller, "preview", request, mock(MagicHttpServletRequest.class)));
	}

	@Test
	void configurationRegistersTheDefaultServicePluginAndAggregatedPluginConfiguration() throws Exception {
		Class<?> configurationClass = Class.forName(CONFIGURATION_CLASS);
		ConditionalOnClass dependencyClasses = configurationClass.getAnnotation(ConditionalOnClass.class);
		assertNotNull(dependencyClasses);
		List<Class<?>> requiredClasses = Arrays.asList(dependencyClasses.value());
		assertTrue(requiredClasses.contains(MagicPluginConfiguration.class));
		assertTrue(requiredClasses.contains(MagicDynamicDataSource.class));
		ConditionalOnProperty enabled = configurationClass.getAnnotation(ConditionalOnProperty.class);
		assertNotNull(enabled);
		assertEquals("magic-api.plugin.db", enabled.prefix());
		assertEquals("enabled", enabled.name()[0]);
		assertEquals("true", enabled.havingValue());
		assertTrue(enabled.matchIfMissing());
		Object configuration = configurationClass.getConstructor(ObjectProvider.class).newInstance(mock(ObjectProvider.class));
		Plugin plugin = (Plugin) configurationClass.getMethod("plugin").invoke(configuration);
		assertEquals("db", plugin.getName());
		assertNull(plugin.getGlobalName());
		assertNull(plugin.getJavascriptFilename());

		DbBrowserSqlBuilder sqlBuilder = (DbBrowserSqlBuilder) configurationClass.getMethod("dbBrowserSqlBuilder").invoke(configuration);
		Object service = configurationClass.getMethod("dbBrowserService", MagicDynamicDataSource.class, DbBrowserSqlBuilder.class)
				.invoke(configuration, new MagicDynamicDataSource(), sqlBuilder);
		assertInstanceOf(DefaultDbBrowserService.class, service);
		assertNotNull(configurationClass.getMethod("controllerRegister").invoke(configuration));

		Import aggregatedPlugins = MagicApiPluginAutoConfiguration.class.getAnnotation(Import.class);
		assertNotNull(aggregatedPlugins);
		assertTrue(Arrays.asList(aggregatedPlugins.value()).contains(configurationClass));
	}

	private void assertEndpoint(Class<?> controllerClass, String methodName, Class<? extends Annotation> mappingType,
			String path, Class<?>... parameterTypes) throws Exception {
		Method method = controllerClass.getMethod(methodName, parameterTypes);
		Annotation mapping = method.getAnnotation(mappingType);
		assertNotNull(mapping);
		String[] paths = mapping instanceof GetMapping ? ((GetMapping) mapping).value() : ((PostMapping) mapping).value();
		assertEquals(path, paths[0]);
		Valid valid = method.getAnnotation(Valid.class);
		assertNotNull(valid);
		assertTrue(valid.requireLogin());
		assertEquals(JsonBean.class, method.getReturnType());
	}

	private Object controller(DbBrowserService service, boolean allowed) throws Exception {
		MagicConfiguration configuration = new MagicConfiguration();
		configuration.setAuthorizationInterceptor(new AuthorizationInterceptor() {
			@Override
			public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization) {
				return allowed;
			}
		});
		Class<?> controllerClass = Class.forName(CONTROLLER_CLASS);
		Constructor<?> constructor = controllerClass.getConstructor(MagicConfiguration.class, DbBrowserService.class);
		return constructor.newInstance(configuration, service);
	}

	private Object invoke(Object target, String methodName, Object... arguments) throws Exception {
		Method method = null;
		for (Method candidate : target.getClass().getMethods()) {
			if (candidate.getName().equals(methodName) && candidate.getParameterTypes().length == arguments.length) {
				method = candidate;
				break;
			}
		}
		assertNotNull(method);
		try {
			return method.invoke(target, arguments);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			}
			throw e;
		}
	}

	private Object data(Object response) {
		return ((JsonBean<?>) response).getData();
	}

}
