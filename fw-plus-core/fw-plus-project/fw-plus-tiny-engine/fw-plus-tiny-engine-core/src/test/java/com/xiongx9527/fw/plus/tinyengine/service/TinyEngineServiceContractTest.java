package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineServiceContractTest {

	private static final String SERVICE_PACKAGE = "com.xiongx9527.fw.plus.tinyengine.service.";
	private static final String IMPLEMENTATION_PACKAGE = SERVICE_PACKAGE + "impl.";
	private static final String HOOK_PACKAGE = "com.xiongx9527.fw.plus.tinyengine.support.hook.";

	private static final List<String> SERVICE_NAMES = Arrays.asList(
			"AppExtensionService",
			"AppService",
			"AppTemplateService",
			"BlockGroupService",
			"BlockHistoryService",
			"BlockService",
			"BusinessCategoryService",
			"CanvasService",
			"ComponentLibraryService",
			"ComponentService",
			"DatasourceService",
			"I18nEntryService",
			"I18nLangService",
			"MaterialHistoryService",
			"MaterialService",
			"ModelService",
			"PageHistoryService",
			"PageService",
			"PageTemplateService",
			"PlatformHistoryService",
			"PlatformService",
			"ResourceGroupService",
			"ResourceService",
			"TaskRecordService",
			"TenantService",
			"UserService"
	);
	private static final List<String> DOMAIN_METHOD_NAMES = Arrays.asList(
			"findAllAppExtension",
			"queryAllApp",
			"queryAllAppTemplate",
			"findAllBlockGroup",
			"findAllBlockHistory",
			"queryAllBlock",
			"queryAllBusinessCategory",
			"lockCanvas",
			"queryAllComponentLibrary",
			"findAllComponent",
			"queryAllDatasource",
			"queryAllI18nEntry",
			"queryAllI18nLang",
			"findAllMaterialHistory",
			"findAllMaterial",
			"queryModelByCondition",
			"findAllPageHistory",
			"queryAllPage",
			"queryAllPageTemplate",
			"queryAllPlatformHistory",
			"queryAllPlatform",
			"queryAllResourceGroupAndResource",
			"queryAllResource",
			"queryAllTaskRecord",
			"findAllTenant",
			"queryAllUser"
	);

	@Test
	void everyPublicServiceInterfaceHasSpringImplementation() {
		for (int index = 0; index < SERVICE_NAMES.size(); index++) {
			String serviceName = SERVICE_NAMES.get(index);
			Class<?> serviceType = loadClass(SERVICE_PACKAGE + serviceName);
			Class<?> implementationType = loadClass(IMPLEMENTATION_PACKAGE + serviceName + "Impl");

			assertTrue(serviceType.isInterface(), serviceName + " must be an interface");
			assertTrue(Modifier.isPublic(serviceType.getModifiers()), serviceName + " must be public");
			assertTrue(serviceType.isAssignableFrom(implementationType),
					implementationType.getName() + " must implement " + serviceType.getName());
			assertNotNull(implementationType.getAnnotation(Service.class),
					implementationType.getName() + " must be a Spring service");
			String domainMethodName = DOMAIN_METHOD_NAMES.get(index);
			assertTrue(Arrays.stream(serviceType.getMethods())
					.anyMatch(method -> domainMethodName.equals(method.getName())),
					serviceType.getName() + " must retain domain method " + domainMethodName);
		}
	}

	@Test
	void serviceContractsAndImplementationsStayInsideCoreBoundary() {
		for (String serviceName : SERVICE_NAMES) {
			Class<?> serviceType = loadClass(SERVICE_PACKAGE + serviceName);
			Class<?> implementationType = loadClass(IMPLEMENTATION_PACKAGE + serviceName + "Impl");

			for (Method method : serviceType.getMethods()) {
				assertAllowedType(method.getReturnType(), serviceName + "#" + method.getName() + " return type");
				for (Class<?> parameterType : method.getParameterTypes()) {
					assertAllowedType(parameterType, serviceName + "#" + method.getName() + " parameter");
				}
			}
			for (Field field : implementationType.getDeclaredFields()) {
				assertNoForbiddenDependency(field.getType(), implementationType.getName() + " field " + field.getName());
			}
			for (Constructor<?> constructor : implementationType.getDeclaredConstructors()) {
				for (Class<?> parameterType : constructor.getParameterTypes()) {
					assertNoForbiddenDependency(parameterType, implementationType.getName() + " constructor");
				}
			}
		}
	}

	@Test
	void defaultSaveHookIsNoop() throws Exception {
		Class<?> hookType = loadClass(HOOK_PACKAGE + "TinyEngineSaveHook");
		Class<?> noopType = loadClass(HOOK_PACKAGE + "NoopTinyEngineSaveHook");
		Object noop = noopType.getDeclaredConstructor().newInstance();
		Method afterSave = hookType.getMethod("afterSave", Page.class);

		assertTrue(hookType.isInterface());
		assertTrue(hookType.isAssignableFrom(noopType));
		assertDoesNotThrow(() -> afterSave.invoke(noop, new Page()));
		assertDoesNotThrow(() -> afterSave.invoke(noop, new Object[] {null}));
	}

	private static Class<?> loadClass(String className) {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException exception) {
			throw new AssertionError("Missing TinyEngine service contract class: " + className, exception);
		}
	}

	private static void assertAllowedType(Class<?> type, String location) {
		Class<?> componentType = type.isArray() ? type.getComponentType() : type;
		String packageName = componentType.isPrimitive() || componentType.getPackage() == null
				? ""
				: componentType.getPackage().getName();
		boolean allowed = componentType.isPrimitive()
				|| packageName.startsWith("java.")
				|| packageName.startsWith("com.xiongx9527.fw.plus.tinyengine.")
				|| packageName.startsWith("com.baomidou.mybatisplus.");
		assertTrue(allowed, location + " leaks non-core type " + type.getName());
		assertNoForbiddenDependency(type, location);
	}

	private static void assertNoForbiddenDependency(Class<?> type, String location) {
		String name = type.getName().toLowerCase();
		assertTrue(!name.contains("controller")
				&& !name.contains("servlet")
				&& !name.contains("swagger")
				&& !name.startsWith("jakarta."), location + " depends on forbidden type " + type.getName());
	}
}
