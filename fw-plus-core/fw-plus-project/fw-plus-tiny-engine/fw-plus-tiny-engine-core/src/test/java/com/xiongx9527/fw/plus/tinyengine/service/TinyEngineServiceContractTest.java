package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiongx9527.fw.plus.tinyengine.common.model.PageQueryVo;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppExtensionMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.DatasourceMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nEntryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nLangMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ModelMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceGroupMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.TaskRecordMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.UserMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.CanvasDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.DeleteI18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryListResult;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nBatchEntries;
import com.xiongx9527.fw.plus.tinyengine.model.dto.OperateI18nEntries;
import com.xiongx9527.fw.plus.tinyengine.model.dto.PublishedPageVo;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaI18n;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nLang;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ResourceGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.TaskRecord;
import com.xiongx9527.fw.plus.tinyengine.model.entity.User;
import com.xiongx9527.fw.plus.tinyengine.service.impl.AppServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.AppTemplateServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.CanvasServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.I18nEntryServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.PageHistoryServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.PageServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.ResourceGroupServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.TaskRecordServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.hook.TinyEngineSaveHook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
	private static final List<String> DOMAIN_METHOD_NAMES = Arrays.asList(
			"findAllAppExtension", "queryAllApp", "queryAllAppTemplate", "findAllBlockGroup",
			"findAllBlockHistory", "queryAllBlock", "queryAllBusinessCategory", "lockCanvas",
			"queryAllComponentLibrary", "findAllComponent", "queryAllDatasource", "findAllI18nEntry",
			"queryAllI18nLang", "findAllMaterialHistory", "findAllMaterial", "queryModelByCondition",
			"findAllPageHistory", "queryAllPage", "queryAllPageTemplate", "queryAllPlatformHistory",
			"queryAllPlatform", "queryAllResourceGroupAndResource", "queryAllResource",
			"queryAllTaskRecord", "findAllTenant", "queryAllUser"
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
	void keyServicesExposeCompleteCoreSafeSignatures() {
		assertMethod(AppTemplateService.class, "queryAllAppTemplate", AppDto.class,
				Integer.class, Integer.class, String.class, App.class);
		assertMethod(AppTemplateService.class, "queryAppTemplateById", App.class, Integer.class);
		assertMethod(AppTemplateService.class, "createAppByTemplate", App.class, App.class);
		assertMethod(AppTemplateService.class, "updateAppTemplateById", Integer.class, App.class);

		assertMethod(PageService.class, "queryAllPage", List.class, Integer.class);
		assertMethod(PageService.class, "queryPageById", Page.class, Integer.class);
		assertMethod(PageService.class, "queryPageByCondition", List.class, Page.class);
		assertMethod(PageService.class, "delPage", Page.class, Integer.class);
		assertMethod(PageService.class, "createPage", Page.class, Page.class);
		assertMethod(PageService.class, "createFolder", Page.class, Page.class);
		assertMethod(PageService.class, "updatePage", Page.class, Page.class);
		assertMethod(PageService.class, "update", Page.class, Page.class);

		assertMethod(PageHistoryService.class, "findAllPageHistory", List.class);
		assertMethod(PageHistoryService.class, "findPageHistoryById", PageHistory.class, Integer.class);
		assertMethod(PageHistoryService.class, "findPageHistoryByCondition", List.class, PageHistory.class);
		assertMethod(PageHistoryService.class, "deletePageHistoryById", Integer.class, Integer.class);
		assertMethod(PageHistoryService.class, "updatePageHistoryById", Integer.class, PageHistory.class);
		assertMethod(PageHistoryService.class, "createPageHistory", Integer.class, PageHistory.class);
		assertMethod(PageHistoryService.class, "findPageHistoryByName", List.class, String.class, Integer.class);
		assertMethod(PageHistoryService.class, "findLatestPublishPage", IPage.class, PageQueryVo.class);
		assertMethod(PageHistoryService.class, "selectMaxVersionOfPageHistory", String.class,
				String.class, Integer.class);

		assertMethod(I18nEntryService.class, "findAllI18nEntry", I18nEntryListResult.class);
		assertMethod(I18nEntryService.class, "findI18nEntryById", I18nEntryDto.class, Integer.class);
		assertMethod(I18nEntryService.class, "findI18nEntryByCondition", List.class, I18nEntry.class);
		assertMethod(I18nEntryService.class, "updateI18nEntryById", Integer.class, I18nEntry.class);
		assertMethod(I18nEntryService.class, "formatEntriesList", SchemaI18n.class, List.class);
		assertMethod(I18nEntryService.class, "create", List.class, OperateI18nEntries.class);
		assertMethod(I18nEntryService.class, "bulkCreate", List.class, OperateI18nBatchEntries.class);
		assertMethod(I18nEntryService.class, "bulkUpdate", List.class, OperateI18nEntries.class);
		assertMethod(I18nEntryService.class, "deleteI18nEntriesByHostAndHostTypeAndKey", List.class,
				DeleteI18nEntry.class);
		assertMethod(I18nEntryService.class, "findI18nEntryByApp", I18nEntryListResult.class,
				Integer.class, String.class);

		assertMethod(TaskRecordService.class, "queryAllTaskRecord", List.class);
		assertMethod(TaskRecordService.class, "queryTaskRecordById", TaskRecord.class, Integer.class);
		assertMethod(TaskRecordService.class, "queryTaskRecordByCondition", List.class, TaskRecord.class);
		assertMethod(TaskRecordService.class, "deleteTaskRecordById", Integer.class, Integer.class);
		assertMethod(TaskRecordService.class, "updateTaskRecordById", Integer.class, TaskRecord.class);
		assertMethod(TaskRecordService.class, "createTaskRecord", Integer.class, TaskRecord.class);
		assertMethod(TaskRecordService.class, "status", List.class, int.class, String.class);
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
	void canvasLockAndUnlockPersistExpectedOccupier() {
		PageMapper pageMapper = mock(PageMapper.class);
		BlockMapper blockMapper = mock(BlockMapper.class);
		UserMapper userMapper = mock(UserMapper.class);
		TinyEngineUserContext context = context("user-7", "tenant-9");
		User user = new User();
		user.setId("user-7");
		when(userMapper.queryUserById("user-7")).thenReturn(user);
		Page page = new Page();
		page.setId(31);
		when(pageMapper.queryPageById(31)).thenReturn(page);
		Block block = new Block();
		block.setId(41);
		block.setOccupierBy("user-7");
		when(blockMapper.queryBlockById(41)).thenReturn(block);
		CanvasServiceImpl service = new CanvasServiceImpl(pageMapper, blockMapper, userMapper, context);

		CanvasDto lockResult = service.lockCanvas(31, "occupy", "page");
		CanvasDto unlockResult = service.lockCanvas(41, "release", "block");

		assertEquals("success", lockResult.getOperate());
		assertSame(user, lockResult.getOccupier());
		assertEquals("success", unlockResult.getOperate());
		assertNull(unlockResult.getOccupier());
		ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
		verify(pageMapper).updatePageById(pageCaptor.capture());
		assertEquals("user-7", pageCaptor.getValue().getOccupierBy());
		ArgumentCaptor<Block> blockCaptor = ArgumentCaptor.forClass(Block.class);
		verify(blockMapper).updateBlockById(blockCaptor.capture());
		assertNull(blockCaptor.getValue().getOccupierBy());
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

	private static Page page(int id, int appId, boolean isPage) {
		Page page = new Page();
		page.setId(id);
		page.setApp(appId);
		page.setIsPage(isPage);
		return page;
	}

	private static TinyEngineUserContext context(String userId, String tenantId) {
		TinyEngineUser user = new TinyEngineUser(userId, "tester", tenantId, "renter", 1);
		return () -> user;
	}

	private static void setBaseMapper(Object service, Object mapper) {
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
	}

	private static void assertMethod(Class<?> owner, String name, Class<?> returnType, Class<?>... parameterTypes) {
		try {
			Method method = owner.getMethod(name, parameterTypes);
			assertEquals(returnType, method.getReturnType(), owner.getSimpleName() + "#" + name + " return type");
		}
		catch (NoSuchMethodException exception) {
			throw new AssertionError("Missing core-safe service method " + owner.getName() + "#" + name, exception);
		}
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
}
