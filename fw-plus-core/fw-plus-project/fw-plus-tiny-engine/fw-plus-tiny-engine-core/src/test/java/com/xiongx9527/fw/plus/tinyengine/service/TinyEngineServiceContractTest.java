package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.common.model.PageQueryVo;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierException;
import com.xiongx9527.fw.plus.tinyengine.mapper.*;
import com.xiongx9527.fw.plus.tinyengine.model.dto.*;
import com.xiongx9527.fw.plus.tinyengine.model.entity.*;
import com.xiongx9527.fw.plus.tinyengine.service.impl.*;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.hook.TinyEngineSaveHook;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TinyEngineServiceContractTest {

	private static final String SERVICE_PACKAGE = "com.xiongx9527.fw.plus.tinyengine.service.";
	private static final String IMPLEMENTATION_PACKAGE = SERVICE_PACKAGE + "impl.";
	private static final String HOOK_PACKAGE = "com.xiongx9527.fw.plus.tinyengine.support.hook.";

	private static final List<String> SERVICE_NAMES = Arrays.asList(
			"AppExtensionService", "AppService", "AppTemplateService", "BlockGroupService",
			"BlockHistoryService", "BlockService", "BusinessCategoryService", "CanvasService",
			"ComponentLibraryService", "ComponentService", "DatasourceService", "I18nEntryService",
			"I18nLangService", "MaterialHistoryService", "MaterialService", "ModelService",
			"PageHistoryService", "PageService", "PageTemplateService", "PlatformHistoryService",
			"PlatformService", "ResourceGroupService", "ResourceService", "TaskRecordService",
			"TenantService", "UserService"
	);
	private static final List<String> MYBATIS_PLUS_SERVICE_NAMES = Arrays.asList(
			"AppExtensionService", "AppService", "AppTemplateService", "BlockGroupService",
			"BlockHistoryService", "BlockService", "BusinessCategoryService", "ComponentLibraryService",
			"ComponentService", "DatasourceService", "I18nEntryService", "I18nLangService",
			"MaterialHistoryService", "MaterialService", "ModelService", "PageHistoryService",
			"PageService", "PageTemplateService", "PlatformHistoryService", "PlatformService",
			"ResourceGroupService", "ResourceService", "TaskRecordService", "TenantService", "UserService"
	);

	@Test
	void everyPublicServiceInterfaceHasSpringImplementation() {
		for (String serviceName : SERVICE_NAMES) {
			Class<?> serviceType = loadClass(SERVICE_PACKAGE + serviceName);
			Class<?> implementationType = loadClass(IMPLEMENTATION_PACKAGE + serviceName + "Impl");

			assertTrue(serviceType.isInterface(), serviceName + " must be an interface");
			assertTrue(Modifier.isPublic(serviceType.getModifiers()), serviceName + " must be public");
			assertTrue(serviceType.isAssignableFrom(implementationType),
					implementationType.getName() + " must implement " + serviceType.getName());
			assertNotNull(implementationType.getAnnotation(Service.class),
					implementationType.getName() + " must be a Spring service");
		}
	}

	@Test
	void everyMyBatisPlusServiceKeepsLegacyPublicParentTypes() {
		for (String serviceName : MYBATIS_PLUS_SERVICE_NAMES) {
			Class<?> serviceType = loadClass(SERVICE_PACKAGE + serviceName);
			Class<?> implementationType = loadClass(IMPLEMENTATION_PACKAGE + serviceName + "Impl");
			Class<?>[] parentInterfaces = serviceType.getInterfaces();

			assertEquals(1, parentInterfaces.length, serviceType.getName() + " direct parent interface count");
			assertEquals("com.baomidou.mybatisplus.extension.service.IService", parentInterfaces[0].getName(),
					serviceType.getName() + " direct parent interface");
			assertEquals("com.baomidou.mybatisplus.extension.service.impl.ServiceImpl",
					implementationType.getSuperclass().getName(), implementationType.getName() + " direct parent class");
		}
	}

	@Test
	void everyServiceExposesCompleteCoreSafeSignatureTable() {
		for (Map.Entry<Class<?>, List<ExpectedMethod>> entry : expectedServiceContracts().entrySet()) {
			Class<?> serviceType = entry.getKey();
			List<ExpectedMethod> expectedMethods = entry.getValue();
			Set<String> actualMethods = new HashSet<String>();
			for (Method method : serviceType.getDeclaredMethods()) {
				actualMethods.add(methodKey(method.getName(), method.getParameterTypes()));
			}
			Set<String> expectedKeys = new HashSet<String>();
			for (ExpectedMethod expected : expectedMethods) {
				expectedKeys.add(methodKey(expected.name, expected.parameterTypes));
				assertMethod(serviceType, expected);
			}
			assertEquals(expectedKeys, actualMethods, serviceType.getSimpleName() + " domain method table");
		}
	}

	@Test
	void queryAllPageCalculatesHomeFromTenantScopedApp() {
		PageMapper pageMapper = mock(PageMapper.class);
		AppMapper appMapper = mock(AppMapper.class);
		TinyEngineUserContext context = context("user-7", "tenant-9");
		Page home = page(10, 7, true);
		Page other = page(11, 7, true);
		Page folder = page(12, 7, false);
		App app = new App();
		app.setHomePage(10);
		when(pageMapper.queryPageByApp(7)).thenReturn(Arrays.asList(home, other, folder));
		when(appMapper.queryAppById(7, "tenant-9")).thenReturn(app);
		PageServiceImpl service = new PageServiceImpl();
		setBaseMapper(service, pageMapper);
		ReflectionTestUtils.setField(service, "appMapper", appMapper);
		ReflectionTestUtils.setField(service, "userContext", context);

		List<Page> result = service.queryAllPage(7);

		assertTrue(result.get(0).getIsHome());
		assertFalse(result.get(1).getIsHome());
		assertNull(result.get(2).getIsHome());
		verify(appMapper).queryAppById(7, "tenant-9");
	}

	@Test
	void templatePaginationCapsPageSizeAndPreventsOffsetOverflow() {
		AppMapper mapper = mock(AppMapper.class);
		AppTemplateServiceImpl service = new AppTemplateServiceImpl(context("user-7", "tenant-9"));
		setBaseMapper(service, mapper);
		when(mapper.queryAllAppTemplate(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Collections.<App>emptyList());

		service.queryAllAppTemplate(Integer.MAX_VALUE, 5000, "id desc", new App());

		verify(mapper).queryAllAppTemplate(eq(1000), eq(Integer.MAX_VALUE), eq(null), eq(null), eq(null),
				eq(null), eq("id desc"), eq(null), eq("tenant-9"));
		verify(mapper).queryAppTemplateTotal("tenant-9");
	}

	@Test
	void templateCloneCopiesCoreDataAndUsesTenantCondition() {
		AppMapper appMapper = mock(AppMapper.class);
		PageService pageService = mock(PageService.class);
		PageHistoryMapper pageHistoryMapper = mock(PageHistoryMapper.class);
		AppExtensionMapper extensionMapper = mock(AppExtensionMapper.class);
		DatasourceMapper datasourceMapper = mock(DatasourceMapper.class);
		I18nEntryMapper i18nEntryMapper = mock(I18nEntryMapper.class);
		ModelMapper modelMapper = mock(ModelMapper.class);
		AppTemplateServiceImpl service = new AppTemplateServiceImpl(context("user-7", "tenant-9"));
		setBaseMapper(service, appMapper);
		ReflectionTestUtils.setField(service, "pageService", pageService);
		ReflectionTestUtils.setField(service, "pageHistoryMapper", pageHistoryMapper);
		ReflectionTestUtils.setField(service, "appExtensionMapper", extensionMapper);
		ReflectionTestUtils.setField(service, "datasourceMapper", datasourceMapper);
		ReflectionTestUtils.setField(service, "i18nEntryMapper", i18nEntryMapper);
		ReflectionTestUtils.setField(service, "modelMapper", modelMapper);
		App request = new App();
		request.setId(3);
		request.setIsTemplate(true);
		request.setSetTemplateBy("owner");
		App persisted = new App();
		persisted.setId(22);
		doAnswer(invocation -> {
			((App) invocation.getArgument(0)).setId(22);
			return 1;
		}).when(appMapper).createApp(request);
		when(appMapper.queryAppById(22, "tenant-9")).thenReturn(persisted);
		when(pageService.queryAllPage(3)).thenReturn(Collections.<Page>emptyList());
		when(pageHistoryMapper.queryPageHistoryByAppId(3)).thenReturn(Collections.<PageHistory>emptyList());
		when(extensionMapper.queryAppExtensionByCondition(any())).thenReturn(Collections.emptyList());
		when(datasourceMapper.queryDatasourceByCondition(any())).thenReturn(Collections.emptyList());
		when(i18nEntryMapper.findI18nEntriesByHostandHostType(3, "app")).thenReturn(Collections.emptyList());
		when(modelMapper.queryModelByCondition(any())).thenReturn(Collections.emptyList());

		App result = invoke(service, "createAppByTemplate", new Class<?>[] {App.class}, request);

		assertSame(persisted, result);
		assertFalse(request.getIsTemplate());
		assertNull(request.getSetTemplateBy());
		verify(appMapper).queryAppById(22, "tenant-9");
	}

	@Test
	void tenantAndUserConditionsArePassedToMappers() {
		TinyEngineUserContext context = context("user-7", "tenant-9");
		AppMapper appMapper = mock(AppMapper.class);
		AppServiceImpl appService = new AppServiceImpl(context);
		setBaseMapper(appService, appMapper);
		ResourceGroupMapper resourceGroupMapper = mock(ResourceGroupMapper.class);
		ResourceGroupServiceImpl resourceGroupService = new ResourceGroupServiceImpl(context);
		setBaseMapper(resourceGroupService, resourceGroupMapper);
		when(appMapper.queryAllApp("tenant-9")).thenReturn(Collections.<App>emptyList());
		when(resourceGroupMapper.queryAllResourceGroupAndResource("user-7"))
				.thenReturn(Collections.<ResourceGroup>emptyList());

		appService.queryAllApp();
		resourceGroupService.queryAllResourceGroupAndResource();

		verify(appMapper).queryAllApp("tenant-9");
		verify(resourceGroupMapper).queryAllResourceGroupAndResource("user-7");
	}

	@Test
	void appPaginationAndWritesKeepTenantBoundary() {
		TinyEngineUserContext context = context("user-7", "tenant-9");
		AppMapper mapper = mock(AppMapper.class);
		AppServiceImpl service = new AppServiceImpl(context);
		setBaseMapper(service, mapper);
		App filter = new App();
		filter.setName("demo");
		when(mapper.queryAllAppByPage(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Collections.<App>emptyList());
		when(mapper.queryAppTotal("tenant-9")).thenReturn(0);
		App created = new App();
		created.setName("new-app");
		when(mapper.queryAppByCondition(created)).thenReturn(Collections.<App>emptyList());
		when(mapper.createApp(created)).thenReturn(1);

		AppDto pageResult = invoke(service, "queryAllAppByPage",
				new Class<?>[] {Integer.class, Integer.class, String.class, App.class},
				Integer.MAX_VALUE, 5000, "id desc", filter);
		App createResult = invoke(service, "createApp", new Class<?>[] {App.class}, created);

		assertEquals(Integer.valueOf(0), pageResult.getTotal());
		assertSame(created, createResult);
		assertEquals("tenant-9", created.getTenantId());
		assertFalse(created.getIsPublish());
		verify(mapper).queryAllAppByPage(1000, Integer.MAX_VALUE, "demo", null, null, null,
				"id desc", null, "tenant-9");
		verify(mapper).createApp(created);
	}

	@Test
	void coreCrudWritesDelegateAndReturnDomainValues() {
		AppExtensionMapper extensionMapper = mock(AppExtensionMapper.class);
		AppExtensionServiceImpl extensionService = new AppExtensionServiceImpl();
		setBaseMapper(extensionService, extensionMapper);
		AppExtension extension = new AppExtension();
		when(extensionMapper.queryAppExtensionByCondition(extension)).thenReturn(Collections.<AppExtension>emptyList());
		when(extensionMapper.createAppExtension(extension)).thenReturn(1);

		BlockHistoryMapper historyMapper = mock(BlockHistoryMapper.class);
		BlockHistoryServiceImpl historyService = new BlockHistoryServiceImpl();
		setBaseMapper(historyService, historyMapper);
		BlockHistory history = new BlockHistory();
		when(historyMapper.createBlockHistory(history)).thenReturn(1);

		PlatformMapper platformMapper = mock(PlatformMapper.class);
		PlatformServiceImpl platformService = new PlatformServiceImpl();
		setBaseMapper(platformService, platformMapper);
		Platform platform = new Platform();
		platform.setName("designer");
		when(platformMapper.createPlatform(platform)).thenReturn(1);

		assertSame(extension, invoke(extensionService, "createAppExtension",
				new Class<?>[] {AppExtension.class}, extension));
		assertEquals(Integer.valueOf(1), invoke(historyService, "createBlockHistory",
				new Class<?>[] {BlockHistory.class}, history));
		assertSame(platform, invoke(platformService, "createPlatform",
				new Class<?>[] {Platform.class}, platform));
		verify(extensionMapper).createAppExtension(extension);
		verify(historyMapper).createBlockHistory(history);
		verify(platformMapper).createPlatform(platform);
	}

	@Test
	void resourceQueriesPassCurrentUserToMapper() {
		ResourceMapper mapper = mock(ResourceMapper.class);
		ResourceServiceImpl service = new ResourceServiceImpl();
		setBaseMapper(service, mapper);
		setFieldIfPresent(service, "userContext", context("user-7", "tenant-9"));
		when(mapper.findResourceByResourceGroupId(12, "user-7"))
				.thenReturn(Collections.<Resource>emptyList());

		List<Resource> result = invoke(service, "queryResourceByResourceGroupId",
				new Class<?>[] {Integer.class}, 12);

		assertTrue(result.isEmpty());
		verify(mapper).findResourceByResourceGroupId(12, "user-7");
	}

	@Test
	void pageSaveInvokesHookOnlyAfterSuccessfulInsert() {
		PageMapper mapper = mock(PageMapper.class);
		TinyEngineSaveHook hook = mock(TinyEngineSaveHook.class);
		PageServiceImpl service = new PageServiceImpl();
		setBaseMapper(service, mapper);
		service.setSaveHook(hook);
		Page page = new Page();
		when(mapper.insert(page)).thenReturn(1);

		assertTrue(service.save(page));

		verify(hook).afterSave(page);
	}

	@Test
	void pageHistoryCreationMarksRecordPublishedAndDelegates() {
		PageHistoryMapper mapper = mock(PageHistoryMapper.class);
		PageHistoryServiceImpl service = new PageHistoryServiceImpl();
		setBaseMapper(service, mapper);
		PageHistory history = new PageHistory();
		when(mapper.createPageHistory(history)).thenReturn(1);

		Integer result = invoke(service, "createPageHistory", new Class<?>[] {PageHistory.class}, history);

		assertEquals(Integer.valueOf(1), result);
		assertTrue(history.getIsPublished());
	}

	@Test
	void i18nBulkCreateExpandsEachLanguageAndPersistsEntries() {
		I18nEntryMapper mapper = mock(I18nEntryMapper.class);
		I18nLangMapper langMapper = mock(I18nLangMapper.class);
		I18nEntryServiceImpl service = new I18nEntryServiceImpl();
		setBaseMapper(service, mapper);
		ReflectionTestUtils.setField(service, "i18nLangMapper", langMapper);
		I18nLang chinese = new I18nLang("zh_CN", "Chinese");
		chinese.setId(1);
		I18nLang english = new I18nLang("en_US", "English");
		english.setId(2);
		when(langMapper.selectList(any())).thenReturn(Arrays.asList(chinese, english));
		OperateI18nEntries operation = new OperateI18nEntries();
		operation.setHost("8");
		operation.setHostType("app");
		operation.setKey("welcome");
		Map<String, String> contents = new LinkedHashMap<String, String>();
		contents.put("zh_CN", "hello-zh");
		contents.put("en_US", "hello-en");
		operation.setContents(contents);

		List<I18nEntry> entries = invoke(service, "create", new Class<?>[] {OperateI18nEntries.class}, operation);

		assertEquals(2, entries.size());
		assertEquals(Integer.valueOf(8), entries.get(0).getHost());
		assertEquals("welcome", entries.get(0).getKey());
		verify(mapper, times(2)).createI18nEntry(any(I18nEntry.class));
	}

	@Test
	void taskStatusPreservesRequestedUniqueIdOrder() {
		TaskRecordMapper mapper = mock(TaskRecordMapper.class);
		TaskRecordServiceImpl service = new TaskRecordServiceImpl();
		setBaseMapper(service, mapper);
		TaskRecord first = new TaskRecord();
		TaskRecord second = new TaskRecord();
		when(mapper.findTaskRecordByTaskIdAndUniqueid(5, 9)).thenReturn(Collections.singletonList(first));
		when(mapper.findTaskRecordByTaskIdAndUniqueid(5, 4)).thenReturn(Collections.singletonList(second));

		List<List<TaskRecord>> result = invoke(service, "status", new Class<?>[] {int.class, String.class}, 5, "9,4");

		assertSame(first, result.get(0).get(0));
		assertSame(second, result.get(1).get(0));
	}

	@Test
	void modelDdlUsesCentralValidatorForTableIdentifier() {
		ModelMapper mapper = mock(ModelMapper.class);
		ModelServiceImpl service = new ModelServiceImpl();
		setBaseMapper(service, mapper);
		Model model = model("users; DROP TABLE audit", new ParametersDto[0]);
		when(mapper.selectById(1)).thenReturn(model);

		TinyEngineSqlIdentifierException exception = assertThrows(TinyEngineSqlIdentifierException.class,
				() -> service.getTableById(1));

		assertEquals("TE_SQL_IDENTIFIER_INVALID", exception.getCode());
		assertTrue(exception.getMessage().contains("table name"));
	}

	@Test
	void modelDdlUsesCentralValidatorForFieldIdentifier() {
		ModelMapper mapper = mock(ModelMapper.class);
		ModelServiceImpl service = new ModelServiceImpl();
		setBaseMapper(service, mapper);
		ParametersDto field = new ParametersDto();
		field.setProp("name) COMMENT 'injected'");
		field.setType("String");
		Model model = model("safe_table", field);
		when(mapper.selectById(1)).thenReturn(model);

		TinyEngineSqlIdentifierException exception = assertThrows(TinyEngineSqlIdentifierException.class,
				() -> service.getTableById(1));

		assertEquals("TE_SQL_IDENTIFIER_INVALID", exception.getCode());
		assertTrue(exception.getMessage().contains("field name"));
	}

	@Test
	void modelDdlRejectsUnsafeDefaultValue() {
		ModelMapper mapper = mock(ModelMapper.class);
		ModelServiceImpl service = new ModelServiceImpl();
		setBaseMapper(service, mapper);
		ParametersDto field = new ParametersDto();
		field.setProp("status");
		field.setType("String");
		field.setDefaultValue("'active'); DROP TABLE audit; --");
		Model model = model("safe_table", field);
		when(mapper.selectById(1)).thenReturn(model);

		ServiceException exception = assertThrows(ServiceException.class, () -> service.getTableById(1));

		assertEquals("CM002", exception.getCode());
	}

	@Test
	void tenantCreationRejectsNonNumericUserBeforeAnyInsert() {
		TenantMapper mapper = mock(TenantMapper.class);
		AuthUsersUnitsRolesMapper relationMapper = mock(AuthUsersUnitsRolesMapper.class);
		TenantServiceImpl service = new TenantServiceImpl();
		setBaseMapper(service, mapper);
		ReflectionTestUtils.setField(service, "userContext", context("not-a-number", "tenant-9"));
		ReflectionTestUtils.setField(service, "authUsersUnitsRolesMapper", relationMapper);
		Tenant tenant = new Tenant();
		tenant.setNameEn("new-tenant");
		doAnswer(invocation -> {
			tenant.setId("12");
			return 1;
		}).when(mapper).createTenant(tenant);

		ServiceException exception = assertThrows(ServiceException.class, () -> service.createTenant(tenant));

		assertEquals("CM342", exception.getCode());
		verify(mapper, never()).createTenant(any(Tenant.class));
		verify(relationMapper, never()).createAuthUsersUnitsRoles(any(AuthUsersUnitsRoles.class));
	}

	@Test
	void tenantCreationRejectsRelationInsertFailureAndIsTransactional() throws Exception {
		TenantMapper mapper = mock(TenantMapper.class);
		AuthUsersUnitsRolesMapper relationMapper = mock(AuthUsersUnitsRolesMapper.class);
		TenantServiceImpl service = new TenantServiceImpl();
		setBaseMapper(service, mapper);
		ReflectionTestUtils.setField(service, "userContext", context("7", "tenant-9"));
		ReflectionTestUtils.setField(service, "authUsersUnitsRolesMapper", relationMapper);
		Tenant tenant = new Tenant();
		tenant.setNameEn("new-tenant");
		doAnswer(invocation -> {
			tenant.setId("12");
			return 1;
		}).when(mapper).createTenant(tenant);
		when(relationMapper.createAuthUsersUnitsRoles(any(AuthUsersUnitsRoles.class))).thenReturn(0);

		ServiceException exception = assertThrows(ServiceException.class, () -> service.createTenant(tenant));

		assertEquals("CM001", exception.getCode());
		verify(mapper).createTenant(tenant);
		verify(relationMapper).createAuthUsersUnitsRoles(any(AuthUsersUnitsRoles.class));
		Transactional transactional = TenantServiceImpl.class.getMethod("createTenant", Tenant.class)
				.getAnnotation(Transactional.class);
		assertNotNull(transactional);
		assertTrue(Arrays.asList(transactional.rollbackFor()).contains(Exception.class));
	}

	@Test
	void tenantCreationRequiresExactlyOneTenantRow() {
		TenantMapper mapper = mock(TenantMapper.class);
		AuthUsersUnitsRolesMapper relationMapper = mock(AuthUsersUnitsRolesMapper.class);
		TenantServiceImpl service = new TenantServiceImpl();
		setBaseMapper(service, mapper);
		ReflectionTestUtils.setField(service, "userContext", context("7", "tenant-9"));
		ReflectionTestUtils.setField(service, "authUsersUnitsRolesMapper", relationMapper);
		Tenant tenant = new Tenant();
		tenant.setNameEn("new-tenant");
		when(mapper.createTenant(tenant)).thenReturn(2);

		ServiceException exception = assertThrows(ServiceException.class, () -> service.createTenant(tenant));

		assertEquals("CM001", exception.getCode());
		verify(relationMapper, never()).createAuthUsersUnitsRoles(any(AuthUsersUnitsRoles.class));
	}

	@Test
	void pageDeletionRejectsAnotherUsersOccupancy() {
		PageMapper mapper = mock(PageMapper.class, invocation -> {
			if ("deletePageById".equals(invocation.getMethod().getName())) {
				return 1;
			}
			if ("deletePageIfAvailable".equals(invocation.getMethod().getName())) {
				return 0;
			}
			return null;
		});
		Page occupiedPage = page(31, 7, true);
		occupiedPage.setOccupierBy("user-8");
		when(mapper.queryPageById(31)).thenReturn(occupiedPage);
		PageServiceImpl service = new PageServiceImpl();
		setBaseMapper(service, mapper);
		ReflectionTestUtils.setField(service, "userContext", context("user-7", "tenant-9"));

		ServiceException exception = assertThrows(ServiceException.class, () -> service.delPage(31));

		assertEquals("CM001", exception.getCode());
		assertInvocation(mapper, "deletePageIfAvailable", 31, "user-7");
		verify(mapper, never()).deletePageById(31);
	}

	@Test
	void canvasLockAndUnlockUseGuardedAtomicUpdates() {
		PageMapper pageMapper = mock(PageMapper.class, invocation ->
				"updatePageOccupierIfAvailable".equals(invocation.getMethod().getName()) ? 1 : null);
		BlockMapper blockMapper = mock(BlockMapper.class, invocation ->
				"updateBlockOccupierIfAvailable".equals(invocation.getMethod().getName()) ? 1 : null);
		UserMapper userMapper = mock(UserMapper.class);
		TinyEngineUserContext context = context("user-7", "tenant-9");
		User user = new User();
		user.setId("user-7");
		when(userMapper.queryUserById("user-7")).thenReturn(user);
		CanvasServiceImpl service = new CanvasServiceImpl(pageMapper, blockMapper, userMapper, context);

		CanvasDto lockResult = service.lockCanvas(31, "occupy", "page");
		CanvasDto unlockResult = service.lockCanvas(41, "release", "block");

		assertEquals("success", lockResult.getOperate());
		assertSame(user, lockResult.getOccupier());
		assertEquals("success", unlockResult.getOperate());
		assertNull(unlockResult.getOccupier());
		assertInvocation(pageMapper, "updatePageOccupierIfAvailable", 31, "user-7", "user-7");
		assertInvocation(blockMapper, "updateBlockOccupierIfAvailable", 41, "user-7", null);
	}

	@Test
	void canvasRejectsConcurrentOccupancyWhenGuardedUpdateAffectsNoRows() {
		PageMapper pageMapper = mock(PageMapper.class, invocation ->
				"updatePageOccupierIfAvailable".equals(invocation.getMethod().getName()) ? 0 : null);
		BlockMapper blockMapper = mock(BlockMapper.class);
		UserMapper userMapper = mock(UserMapper.class);
		TinyEngineUserContext context = context("user-7", "tenant-9");
		Page page = new Page();
		page.setId(31);
		when(pageMapper.queryPageById(31)).thenReturn(page);
		CanvasServiceImpl service = new CanvasServiceImpl(pageMapper, blockMapper, userMapper, context);

		CanvasDto result = service.lockCanvas(31, "occupy", "page");

		assertEquals("failed", result.getOperate());
		assertNull(result.getOccupier());
		assertInvocation(pageMapper, "updatePageOccupierIfAvailable", 31, "user-7", "user-7");
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

	private static Map<Class<?>, List<ExpectedMethod>> expectedServiceContracts() {
		Map<Class<?>, List<ExpectedMethod>> contracts = new LinkedHashMap<Class<?>, List<ExpectedMethod>>();
		contracts.put(AppExtensionService.class, Arrays.asList(
				method("findAllAppExtension", generic(List.class, AppExtension.class)),
				method("findAppExtensionById", type(AppExtension.class), Integer.class),
				method("findAppExtensionByCondition", generic(List.class, AppExtension.class), AppExtension.class),
				method("deleteAppExtensionById", type(AppExtension.class), Integer.class),
				method("updateAppExtensionById", type(AppExtension.class), AppExtension.class),
				method("createAppExtension", type(AppExtension.class), AppExtension.class)));
		contracts.put(AppService.class, Arrays.asList(
				method("queryAllApp", generic(List.class, App.class)),
				method("queryAllAppByPage", type(AppDto.class), Integer.class, Integer.class, String.class, App.class),
				method("queryAppById", type(App.class), Integer.class),
				method("queryAppByCondition", generic(List.class, App.class), App.class),
				method("deleteAppById", type(App.class), Integer.class),
				method("updateAppById", type(App.class), App.class),
				method("createApp", type(App.class), App.class),
				method("formatI18nEntrites", type(SchemaI18n.class), List.class, Integer.class, Integer.class)));
		contracts.put(AppTemplateService.class, Arrays.asList(
				method("queryAllAppTemplate", type(AppDto.class), Integer.class, Integer.class, String.class, App.class),
				method("queryAppTemplateById", type(App.class), Integer.class),
				method("createAppByTemplate", type(App.class), App.class),
				method("updateAppTemplateById", type(Integer.class), App.class)));
		contracts.put(BlockGroupService.class, Arrays.asList(
				method("findAllBlockGroup", generic(List.class, BlockGroup.class)),
				method("findBlockGroupById", type(BlockGroup.class), Integer.class),
				method("findBlockGroupByCondition", generic(List.class, BlockGroupDto.class), BlockGroup.class),
				method("deleteBlockGroupById", type(Integer.class), Integer.class),
				method("updateBlockGroupById", type(Integer.class), BlockGroup.class),
				method("createBlockGroup", type(BlockGroup.class), BlockGroup.class),
				method("getBlockGroupByIdsOrAppId", generic(List.class, BlockGroup.class),
						List.class, Integer.class, String.class)));
		contracts.put(BlockHistoryService.class, Arrays.asList(
				method("findAllBlockHistory", generic(List.class, BlockHistory.class)),
				method("findBlockHistoryById", type(BlockHistory.class), Integer.class),
				method("findBlockHistoryByCondition", generic(List.class, BlockHistory.class), BlockHistory.class),
				method("deleteBlockHistoryById", type(Integer.class), Integer.class),
				method("updateBlockHistoryById", type(Integer.class), BlockHistory.class),
				method("createBlockHistory", type(Integer.class), BlockHistory.class)));
		contracts.put(BlockService.class, Arrays.asList(
				method("queryAllBlock", generic(List.class, Block.class)),
				method("queryBlockById", type(BlockDto.class), Integer.class),
				method("queryBlockByCondition", generic(List.class, Block.class), Block.class),
				method("deleteBlockById", type(Integer.class), Integer.class),
				method("updateBlockById", type(BlockDto.class), BlockParam.class),
				method("createBlock", type(BlockDto.class), BlockParam.class),
				method("findBlocksByPagetionList", generic(IPage.class, Block.class), BlockParamDto.class),
				method("allTags", generic(List.class, String.class)),
				method("findBlocksByConditionPagetion", generic(IPage.class, Block.class), Map.class),
				method("getUsers", generic(List.class, User.class), List.class),
				method("listNew", generic(List.class, Block.class), String.class, String.class),
				method("getNotInGroupBlocks", generic(List.class, BlockDto.class), NotGroupDto.class),
				method("getBlockByLabel", type(BlockDto.class), String.class, Integer.class),
				method("deploy", type(BlockDto.class), BlockBuildDto.class)));
		contracts.put(BusinessCategoryService.class, Arrays.asList(
				method("queryAllBusinessCategory", generic(List.class, BusinessCategory.class)),
				method("queryBusinessCategoryById", type(BusinessCategory.class), Integer.class),
				method("queryBusinessCategoryByCondition", generic(List.class, BusinessCategory.class),
						BusinessCategory.class),
				method("deleteBusinessCategoryById", type(Integer.class), Integer.class),
				method("updateBusinessCategoryById", type(Integer.class), BusinessCategory.class),
				method("createBusinessCategory", type(Integer.class), BusinessCategory.class)));
		contracts.put(CanvasService.class, Collections.singletonList(
				method("lockCanvas", type(CanvasDto.class), Integer.class, String.class, String.class)));
		contracts.put(ComponentLibraryService.class, Arrays.asList(
				method("queryAllComponentLibrary", generic(List.class, ComponentLibrary.class)),
				method("queryComponentLibraryById", type(ComponentLibrary.class), Integer.class),
				method("queryComponentLibraryByCondition", generic(List.class, ComponentLibrary.class),
						ComponentLibrary.class),
				method("deleteComponentLibraryById", type(ComponentLibrary.class), Integer.class),
				method("updateComponentLibraryById", type(ComponentLibrary.class), ComponentLibrary.class),
				method("createComponentLibrary", type(ComponentLibrary.class), ComponentLibrary.class)));
		contracts.put(ComponentService.class, Arrays.asList(
				method("findAllComponent", generic(List.class, Component.class)),
				method("findComponentById", type(Component.class), Integer.class),
				method("findComponentByCondition", generic(List.class, Component.class), Component.class),
				method("deleteComponentById", type(Integer.class), Integer.class),
				method("updateComponentById", type(Integer.class), Component.class),
				method("createComponent", type(Integer.class), Component.class),
				method("parseBundle", type(BundleResultDto.class), BundleDto.class),
				method("bulkCreate", type(FileResult.class), List.class),
				method("custComponentBatchCreate", type(FileResult.class), CustComponentDto.class)));
		contracts.put(DatasourceService.class, Arrays.asList(
				method("queryAllDatasource", generic(List.class, Datasource.class)),
				method("queryDatasourceById", type(Datasource.class), Integer.class),
				method("queryDatasourceByCondition", generic(List.class, Datasource.class), Datasource.class),
				method("deleteDatasourceById", type(Datasource.class), Integer.class),
				method("updateDatasourceById", type(Datasource.class), Datasource.class),
				method("createDatasource", type(Datasource.class), Datasource.class)));
		contracts.put(I18nEntryService.class, Arrays.asList(
				method("findAllI18nEntry", type(I18nEntryListResult.class)),
				method("findI18nEntryById", type(I18nEntryDto.class), Integer.class),
				method("findI18nEntryByCondition", generic(List.class, I18nEntryDto.class), I18nEntry.class),
				method("updateI18nEntryById", type(Integer.class), I18nEntry.class),
				method("formatEntriesList", type(SchemaI18n.class), List.class),
				method("create", generic(List.class, I18nEntry.class), OperateI18nEntries.class),
				method("bulkCreate", generic(List.class, I18nEntry.class), OperateI18nBatchEntries.class),
				method("bulkUpdate", generic(List.class, I18nEntry.class), OperateI18nEntries.class),
				method("deleteI18nEntriesByHostAndHostTypeAndKey", generic(List.class, I18nEntryDto.class),
						DeleteI18nEntry.class),
				method("findI18nEntryByApp", type(I18nEntryListResult.class), Integer.class, String.class)));
		contracts.put(I18nLangService.class, Arrays.asList(
				method("queryAllI18nLang", generic(List.class, I18nLang.class)),
				method("queryI18nLangById", type(I18nLang.class), Integer.class),
				method("queryI18nLangByCondition", generic(List.class, I18nLang.class), I18nLang.class),
				method("deleteI18nLangById", type(Integer.class), Integer.class),
				method("updateI18nLangById", type(Integer.class), I18nLang.class),
				method("createI18nLang", type(Integer.class), I18nLang.class)));
		contracts.put(MaterialHistoryService.class, Arrays.asList(
				method("findAllMaterialHistory", generic(List.class, MaterialHistory.class)),
				method("findMaterialHistoryById", type(MaterialHistory.class), Integer.class),
				method("findMaterialHistoryByCondition", generic(List.class, MaterialHistory.class), MaterialHistory.class),
				method("deleteMaterialHistoryById", type(MaterialHistory.class), Integer.class),
				method("updateMaterialHistoryById", type(MaterialHistory.class), MaterialHistory.class),
				method("createMaterialHistory", type(MaterialHistory.class), MaterialHistory.class)));
		contracts.put(MaterialService.class, Arrays.asList(
				method("queryAllMaterial", generic(List.class, Material.class)),
				method("queryMaterialById", type(Material.class), Integer.class),
				method("queryMaterialByCondition", generic(List.class, Material.class), Material.class),
				method("deleteMaterialById", type(Material.class), Integer.class),
				method("updateMaterialById", type(Material.class), Material.class),
				method("createMaterial", type(Material.class), Material.class)));
		contracts.put(ModelService.class, Arrays.asList(
				method("queryModelById", type(Model.class), Integer.class),
				method("getModelByName", generic(List.class, Model.class), String.class),
				method("getModelByEnName", generic(List.class, Model.class), String.class),
				method("pageQuery", generic(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class, Model.class),
						int.class, int.class, String.class, String.class),
				method("getTableById", type(String.class), Integer.class),
				method("getAllTable", type(String.class)),
				method("getAllModelName", generic(List.class, String.class)),
				method("queryModelByCondition", generic(List.class, Model.class), Model.class)));
		contracts.put(PageHistoryService.class, Arrays.asList(
				method("findAllPageHistory", generic(List.class, PageHistory.class)),
				method("findPageHistoryById", type(PageHistory.class), Integer.class),
				method("findPageHistoryByCondition", generic(List.class, PageHistory.class), PageHistory.class),
				method("deletePageHistoryById", type(Integer.class), Integer.class),
				method("updatePageHistoryById", type(Integer.class), PageHistory.class),
				method("createPageHistory", type(Integer.class), PageHistory.class),
				method("findPageHistoryByName", generic(List.class, PageHistory.class), String.class, Integer.class),
				method("findLatestPublishPage", generic(IPage.class, PublishedPageVo.class), PageQueryVo.class),
				method("selectMaxVersionOfPageHistory", type(String.class), String.class, Integer.class)));
		contracts.put(PageService.class, Arrays.asList(
				method("queryAllPage", generic(List.class, Page.class), Integer.class),
				method("queryPageById", type(Page.class), Integer.class),
				method("queryPageByCondition", generic(List.class, Page.class), Page.class),
				method("delPage", type(Page.class), Integer.class),
				method("createPage", type(Page.class), Page.class),
				method("createFolder", type(Page.class), Page.class),
				method("updatePage", type(Page.class), Page.class),
				method("update", type(Page.class), Page.class)));
		contracts.put(PageTemplateService.class, Arrays.asList(
				method("queryAllPageTemplate", generic(List.class, PageTemplate.class), String.class, String.class),
				method("queryPageTemplateById", type(PageTemplate.class), Integer.class),
				method("queryPageTemplateByCondition", generic(List.class, PageTemplate.class), PageTemplate.class),
				method("deletePageTemplateByIds", type(Integer.class), List.class),
				method("updatePageTemplateById", type(Integer.class), PageTemplate.class),
				method("createPageTemplate", type(PageTemplate.class), PageTemplate.class)));
		contracts.put(PlatformHistoryService.class, Arrays.asList(
				method("queryAllPlatformHistory", generic(List.class, PlatformHistory.class)),
				method("queryPlatformHistoryById", type(PlatformHistory.class), Integer.class),
				method("queryPlatformHistoryByCondition", generic(List.class, PlatformHistory.class), PlatformHistory.class),
				method("deletePlatformHistoryById", type(PlatformHistory.class), Integer.class),
				method("updatePlatformHistoryById", type(PlatformHistory.class), PlatformHistory.class),
				method("createPlatformHistory", type(PlatformHistory.class), PlatformHistory.class)));
		contracts.put(PlatformService.class, Arrays.asList(
				method("queryAllPlatform", generic(List.class, Platform.class)),
				method("queryPlatformById", type(Platform.class), Integer.class),
				method("queryPlatformByCondition", generic(List.class, Platform.class), Platform.class),
				method("deletePlatformById", type(Platform.class), Integer.class),
				method("updatePlatformById", type(Platform.class), Platform.class),
				method("createPlatform", type(Platform.class), Platform.class)));
		contracts.put(ResourceGroupService.class, Arrays.asList(
				method("queryAllResourceGroupAndResource", generic(List.class, ResourceGroup.class)),
				method("queryResourceGroupByAppId", generic(List.class, ResourceGroup.class), Integer.class),
				method("queryResourceGroupById", type(ResourceGroup.class), Integer.class),
				method("queryResourceGroupByCondition", generic(List.class, ResourceGroup.class), ResourceGroup.class),
				method("deleteResourceGroupById", type(ResourceGroup.class), Integer.class),
				method("updateResourceGroupById", type(ResourceGroup.class), ResourceGroup.class),
				method("createResourceGroup", type(ResourceGroup.class), ResourceGroup.class)));
		contracts.put(ResourceService.class, Arrays.asList(
				method("queryAllResource", generic(List.class, Resource.class)),
				method("queryResourcesByNameAndDes", generic(List.class, Resource.class), String.class, String.class),
				method("queryResourceById", type(Resource.class), Integer.class),
				method("queryResourceByName", type(Resource.class), String.class),
				method("queryResourceByResourceGroupId", generic(List.class, Resource.class), Integer.class),
				method("deleteResourceById", type(Resource.class), Integer.class),
				method("updateResourceById", type(Resource.class), Resource.class)));
		contracts.put(TaskRecordService.class, Arrays.asList(
				method("queryAllTaskRecord", generic(List.class, TaskRecord.class)),
				method("queryTaskRecordById", type(TaskRecord.class), Integer.class),
				method("queryTaskRecordByCondition", generic(List.class, TaskRecord.class), TaskRecord.class),
				method("deleteTaskRecordById", type(Integer.class), Integer.class),
				method("updateTaskRecordById", type(Integer.class), TaskRecord.class),
				method("createTaskRecord", type(Integer.class), TaskRecord.class),
				method("status", generic(List.class, generic(List.class, TaskRecord.class)), int.class, String.class)));
		contracts.put(TenantService.class, Arrays.asList(
				method("findAllTenant", generic(List.class, Tenant.class)),
				method("findTenantById", type(Tenant.class), Integer.class),
				method("findTenantByCondition", generic(List.class, Tenant.class), Tenant.class),
				method("deleteTenantById", type(Integer.class), Integer.class),
				method("updateTenantById", type(Integer.class), Tenant.class),
				method("createTenant", type(Integer.class), Tenant.class)));
		contracts.put(UserService.class, Arrays.asList(
				method("queryAllUser", generic(List.class, User.class)),
				method("queryUserById", type(User.class), String.class),
				method("queryUserByCondition", generic(List.class, User.class), User.class),
				method("deleteUserById", type(Integer.class), String.class),
				method("updateUserById", type(Integer.class), User.class)));
		return contracts;
	}

	private static Page page(int id, int appId, boolean isPage) {
		Page page = new Page();
		page.setId(id);
		page.setApp(appId);
		page.setIsPage(isPage);
		return page;
	}

	private static Model model(String nameEn, ParametersDto... parameters) {
		Model model = new Model();
		model.setNameEn(nameEn);
		model.setParameters(Arrays.asList(parameters));
		return model;
	}

	private static TinyEngineUserContext context(String userId, String tenantId) {
		TinyEngineUser user = new TinyEngineUser(userId, "tester", tenantId, "renter", 1);
		return () -> user;
	}

	private static void setBaseMapper(Object service, Object mapper) {
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
	}

	private static void setFieldIfPresent(Object target, String name, Object value) {
		if (org.springframework.util.ReflectionUtils.findField(target.getClass(), name) != null) {
			ReflectionTestUtils.setField(target, name, value);
		}
	}

	private static void assertMethod(Class<?> owner, ExpectedMethod expected) {
		try {
			Method method = owner.getDeclaredMethod(expected.name, expected.parameterTypes);
			assertEquals(expected.returnType.rawType, method.getReturnType(),
					owner.getSimpleName() + "#" + expected.name + " raw return type");
			assertEquals(expected.returnType.typeName(), method.getGenericReturnType().getTypeName(),
					owner.getSimpleName() + "#" + expected.name + " generic return type");
		}
		catch (NoSuchMethodException exception) {
			throw new AssertionError("Missing core-safe service method " + owner.getName() + "#" + expected.name,
					exception);
		}
	}

	private static ExpectedMethod method(String name, ExpectedType returnType, Class<?>... parameterTypes) {
		return new ExpectedMethod(name, returnType, parameterTypes);
	}

	private static ExpectedType type(Class<?> rawType) {
		return new ExpectedType(rawType, Collections.<ExpectedType>emptyList());
	}

	private static ExpectedType generic(Class<?> rawType, Class<?> argumentType) {
		return generic(rawType, type(argumentType));
	}

	private static ExpectedType generic(Class<?> rawType, ExpectedType argumentType) {
		return new ExpectedType(rawType, Collections.singletonList(argumentType));
	}

	private static String methodKey(String name, Class<?>[] parameterTypes) {
		StringBuilder key = new StringBuilder(name).append('(');
		for (int index = 0; index < parameterTypes.length; index++) {
			if (index > 0) {
				key.append(',');
			}
			key.append(parameterTypes[index].getName());
		}
		return key.append(')').toString();
	}

	private static void assertInvocation(Object mockObject, String methodName, Object... arguments) {
		for (Invocation invocation : org.mockito.Mockito.mockingDetails(mockObject).getInvocations()) {
			if (methodName.equals(invocation.getMethod().getName())
					&& Arrays.equals(arguments, invocation.getArguments())) {
				return;
			}
		}
		throw new AssertionError("Missing mapper invocation " + methodName + Arrays.toString(arguments));
	}

	@SuppressWarnings("unchecked")
	private static <T> T invoke(Object target, String name, Class<?>[] parameterTypes, Object... arguments) {
		try {
			return (T) target.getClass().getMethod(name, parameterTypes).invoke(target, arguments);
		}
		catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new AssertionError("Invocation failed for " + name, cause);
		}
		catch (ReflectiveOperationException exception) {
			throw new AssertionError("Missing service behavior " + target.getClass().getName() + "#" + name, exception);
		}
	}

	private static Class<?> loadClass(String className) {
		try {
			return Class.forName(className);
		}
		catch (ClassNotFoundException exception) {
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

	private static final class ExpectedMethod {
		private final String name;
		private final ExpectedType returnType;
		private final Class<?>[] parameterTypes;

		private ExpectedMethod(String name, ExpectedType returnType, Class<?>[] parameterTypes) {
			this.name = name;
			this.returnType = returnType;
			this.parameterTypes = parameterTypes;
		}
	}

	private static final class ExpectedType {
		private final Class<?> rawType;
		private final List<ExpectedType> arguments;

		private ExpectedType(Class<?> rawType, List<ExpectedType> arguments) {
			this.rawType = rawType;
			this.arguments = arguments;
		}

		private String typeName() {
			if (arguments.isEmpty()) {
				return rawType.getName();
			}
			StringBuilder name = new StringBuilder(rawType.getName()).append('<');
			for (int index = 0; index < arguments.size(); index++) {
				if (index > 0) {
					name.append(", ");
				}
				name.append(arguments.get(index).typeName());
			}
			return name.append('>').toString();
		}
	}
}
