package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.common.exception.ServiceException;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockCarriersRelationMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupBlockMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockGroupMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.BlockHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.DatasourceMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.I18nEntryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ModelMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageHistoryMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockGroup;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.dto.I18nEntryDto;
import com.xiongx9527.fw.plus.tinyengine.model.entity.I18nEntry;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.service.impl.AppServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.AppTemplateServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.BlockGroupServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.BlockHistoryServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.DatasourceServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.I18nEntryServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.ModelServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.PageServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.PageHistoryServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.ResourceServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppRoleBoundaryRegressionTest {

	@Test
	void appScopedServicesDoNotExposeGenericCrud() throws Exception {
		Class<?>[] serviceTypes = {
				AppService.class, AppTemplateService.class, AppExtensionService.class, BlockService.class,
				BlockGroupService.class, BlockHistoryService.class, DatasourceService.class, I18nEntryService.class,
				ModelService.class, PageService.class, PageHistoryService.class, ResourceService.class,
				ResourceGroupService.class,
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.AppServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.AppTemplateServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.AppExtensionServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.BlockServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.BlockGroupServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.BlockHistoryServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.DatasourceServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.I18nEntryServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.ModelServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.PageServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.PageHistoryServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.ResourceServiceImpl"),
				Class.forName("com.xiongx9527.fw.plus.tinyengine.service.impl.ResourceGroupServiceImpl")
		};

		for (Class<?> serviceType : serviceTypes) {
			assertFalse(IService.class.isAssignableFrom(serviceType), serviceType.getName());
		}
	}

	@Test
	void modelCreationRequiresManagePermission() {
		ModelMapper mapper = mock(ModelMapper.class);
		ModelServiceImpl service = new ModelServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceAllowingOnly(AppPermission.MANAGE));
		when(mapper.createModel(any(Model.class))).thenReturn(1);
		Model model = new Model();
		model.setAppId(1);
		model.setNameCn("客户");
		model.setNameEn("customer");

		service.save(model);

		verify(mapper).createModel(model);
	}

	@Test
	void resourceUpdateAuthorizesThePersistedApplication() {
		ResourceMapper mapper = mock(ResourceMapper.class);
		ResourceServiceImpl service = new ResourceServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		injectIfPresent(service, "appRoleService", permissionServiceDenyingApp(2));
		Resource stored = new Resource();
		stored.setId(4);
		stored.setAppId(2);
		when(mapper.queryResourceById(4)).thenReturn(stored);
		Resource request = new Resource();
		request.setId(4);
		request.setAppId(1);

		assertThrows(TinyEngineAccessDeniedException.class, () -> service.updateResourceById(request));

		verify(mapper, never()).updateResourceById(any(Resource.class));
	}

	@Test
	void appConditionQueryAuthorizesEveryReturnedApplication() {
		AppMapper mapper = mock(AppMapper.class);
		AppServiceImpl service = new AppServiceImpl(context());
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceDenyingApp(2));
		App result = new App();
		result.setId(2);
		when(mapper.queryAppByCondition(any(App.class))).thenReturn(Collections.singletonList(result));

		assertThrows(TinyEngineAccessDeniedException.class, () -> service.queryAppByCondition(new App()));
	}

	@Test
	void templateUpdateRejectsAnOrdinaryApplication() {
		AppMapper mapper = mock(AppMapper.class);
		AppTemplateServiceImpl service = new AppTemplateServiceImpl(context());
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		App request = new App();
		request.setId(5);
		request.setTenantId("tenant-spoofed");
		request.setIsPublish(true);

		assertThrows(ServiceException.class, () -> service.updateAppTemplateById(request));

		verify(mapper, never()).updateAppById(any(App.class));
	}

	@Test
	void pageSaveUsesThePersistedApplicationScope() {
		PageMapper mapper = mock(PageMapper.class);
		AppMapper appMapper = mock(AppMapper.class);
		PageServiceImpl service = new PageServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "appMapper", appMapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceAllowingOnly(AppPermission.DEVELOP));
		App app = persistedAppScope(1);
		when(appMapper.queryAppById(1, "tenant-9")).thenReturn(app);
		when(mapper.insert(any(Page.class))).thenReturn(1);
		Page request = new Page();
		request.setApp(1);
		request.setTenantId("tenant-spoofed");
		request.setRenterId("renter-spoofed");
		request.setSiteId("site-spoofed");

		service.save(request);

		verify(mapper).insert(argThat(value -> "tenant-9".equals(value.getTenantId())
				&& "renter-9".equals(value.getRenterId()) && "site-9".equals(value.getSiteId())));
	}

	@Test
	void resourceSaveUsesThePersistedApplicationScope() {
		ResourceMapper mapper = mock(ResourceMapper.class);
		AppMapper appMapper = mock(AppMapper.class);
		ResourceServiceImpl service = new ResourceServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceAllowingOnly(AppPermission.DEVELOP));
		injectIfPresent(service, "appMapper", appMapper);
		when(appMapper.queryAppById(1, "tenant-9")).thenReturn(persistedAppScope(1));
		when(mapper.insert(any(Resource.class))).thenReturn(1);
		Resource request = new Resource();
		request.setAppId(1);
		request.setTenantId("tenant-spoofed");
		request.setRenterId("renter-spoofed");
		request.setSiteId("site-spoofed");

		service.save(request);

		verify(mapper).insert(argThat(value -> "tenant-9".equals(value.getTenantId())
				&& "renter-9".equals(value.getRenterId()) && "site-9".equals(value.getSiteId())));
	}

	@Test
	void datasourceUpdateKeepsThePersistedApplication() {
		DatasourceMapper mapper = mock(DatasourceMapper.class);
		DatasourceServiceImpl service = new DatasourceServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceDenyingApp(2));
		Datasource stored = new Datasource();
		stored.setId(6);
		stored.setApp(1);
		stored.setTenantId("tenant-9");
		when(mapper.queryDatasourceById(6)).thenReturn(stored);
		when(mapper.updateDatasourceById(any(Datasource.class))).thenReturn(1);
		Datasource request = new Datasource();
		request.setId(6);
		request.setApp(2);

		service.updateDatasourceById(request);

		verify(mapper).updateDatasourceById(argThat(value -> Integer.valueOf(1).equals(value.getApp())));
	}

	@Test
	void i18nUpdateKeepsThePersistedScope() {
		I18nEntryMapper mapper = mock(I18nEntryMapper.class);
		I18nEntryServiceImpl service = new I18nEntryServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceAllowingOnly(AppPermission.DEVELOP));
		I18nEntryDto stored = new I18nEntryDto();
		stored.setId(8);
		stored.setHost(1);
		stored.setHostType("app");
		stored.setTenantId("tenant-9");
		stored.setRenterId("renter-9");
		stored.setSiteId("site-9");
		when(mapper.queryI18nEntryById(8)).thenReturn(stored);
		when(mapper.updateI18nEntryById(any(I18nEntry.class))).thenReturn(1);
		I18nEntry request = new I18nEntry();
		request.setId(8);
		request.setHost(2);
		request.setHostType("block");
		request.setTenantId("tenant-spoofed");
		request.setRenterId("renter-spoofed");
		request.setSiteId("site-spoofed");

		service.updateI18nEntryById(request);

		verify(mapper).updateI18nEntryById(argThat(value -> Integer.valueOf(1).equals(value.getHost())
				&& "app".equals(value.getHostType()) && "tenant-9".equals(value.getTenantId())
				&& "renter-9".equals(value.getRenterId()) && "site-9".equals(value.getSiteId())));
	}

	@Test
	void blockGroupUpdateAuthorizesThePersistedApplication() {
		BlockGroupMapper mapper = mock(BlockGroupMapper.class);
		BlockGroupBlockMapper relationMapper = mock(BlockGroupBlockMapper.class);
		BlockCarriersRelationMapper carriersMapper = mock(BlockCarriersRelationMapper.class);
		AppRoleService appRoleService = permissionServiceDenyingApp(2);
		BlockGroupServiceImpl service = new BlockGroupServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "blockGroupBlockMapper", relationMapper);
		ReflectionTestUtils.setField(service, "blockCarriersRelationMapper", carriersMapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		injectIfPresent(service, "appRoleService", appRoleService);
		BlockGroup stored = new BlockGroup();
		stored.setId(9);
		stored.setAppId(2);
		stored.setTenantId("tenant-9");
		when(mapper.queryBlockGroupAndBlockById(9, null, null)).thenReturn(stored);
		when(relationMapper.findBlockGroupBlockByBlockGroupId(9)).thenReturn(Collections.emptyList());
		when(mapper.updateBlockGroupById(any(BlockGroup.class))).thenReturn(1);
		BlockGroup request = new BlockGroup();
		request.setId(9);
		request.setAppId(1);

		assertThrows(TinyEngineAccessDeniedException.class, () -> service.updateBlockGroupById(request));

		verify(mapper, never()).updateBlockGroupById(any(BlockGroup.class));
	}

	@Test
	void blockGroupListReturnsOnlyGroupsFromVisibleApplications() {
		BlockGroupMapper mapper = mock(BlockGroupMapper.class);
		AppMapper appMapper = mock(AppMapper.class);
		BlockGroupServiceImpl service = new BlockGroupServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		injectIfPresent(service, "appMapper", appMapper);
		BlockGroup visibleGroup = new BlockGroup();
		visibleGroup.setId(1);
		visibleGroup.setAppId(1);
		BlockGroup hiddenGroup = new BlockGroup();
		hiddenGroup.setId(2);
		hiddenGroup.setAppId(2);
		App visibleApp = new App();
		visibleApp.setId(1);
		when(mapper.queryAllBlockGroup()).thenReturn(Arrays.asList(visibleGroup, hiddenGroup));
		when(appMapper.queryVisibleApps("tenant-9", "user-7", false))
				.thenReturn(Collections.singletonList(visibleApp));

		assertEquals(Collections.singletonList(visibleGroup), service.findAllBlockGroup());
	}

	@Test
	void blockGroupAggregateReturnsOnlyGroupsFromVisibleApplicationsWhenAppIdIsAbsent() {
		BlockGroupMapper mapper = mock(BlockGroupMapper.class);
		AppMapper appMapper = mock(AppMapper.class);
		BlockGroupServiceImpl service = new BlockGroupServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		injectIfPresent(service, "appMapper", appMapper);
		BlockGroup visibleGroup = new BlockGroup();
		visibleGroup.setId(1);
		visibleGroup.setAppId(1);
		BlockGroup hiddenGroup = new BlockGroup();
		hiddenGroup.setId(2);
		hiddenGroup.setAppId(2);
		App visibleApp = new App();
		visibleApp.setId(1);
		when(mapper.queryAllBlockGroupAndBlock(null, null))
				.thenReturn(Arrays.asList(visibleGroup, hiddenGroup));
		when(appMapper.queryVisibleApps("tenant-9", "user-7", false))
				.thenReturn(Collections.singletonList(visibleApp));

		assertEquals(Collections.singletonList(visibleGroup),
				service.getBlockGroupByIdsOrAppId(null, null, null));
	}

	@Test
	void blockGroupReadUsesApplicationRoleInsteadOfGroupCreator() {
		BlockGroupMapper mapper = mock(BlockGroupMapper.class);
		BlockGroupServiceImpl service = new BlockGroupServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceAllowingOnly(AppPermission.READ));
		BlockGroup stored = new BlockGroup();
		stored.setId(9);
		stored.setAppId(1);
		stored.setCreatedBy("system");
		when(mapper.queryBlockGroupAndBlockById(9, null, null)).thenReturn(stored);

		assertEquals(stored, service.findBlockGroupById(9));
	}

	@Test
	void blockHistoryUpdateAuthorizesThePersistedApplication() {
		BlockHistoryMapper mapper = mock(BlockHistoryMapper.class);
		BlockHistoryServiceImpl service = new BlockHistoryServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		injectIfPresent(service, "appRoleService", permissionServiceDenyingApp(2));
		BlockHistory stored = new BlockHistory();
		stored.setId(10);
		stored.setAppId(2);
		when(mapper.queryBlockHistoryById(10)).thenReturn(stored);
		BlockHistory request = new BlockHistory();
		request.setId(10);
		request.setAppId(1);

		assertThrows(TinyEngineAccessDeniedException.class, () -> service.updateBlockHistoryById(request));

		verify(mapper, never()).updateBlockHistoryById(any(BlockHistory.class));
	}

	@Test
	void pageHistoryUpdateRejectsARequestThatSpoofsAnotherApplication() {
		PageHistoryMapper mapper = mock(PageHistoryMapper.class);
		PageHistoryServiceImpl service = new PageHistoryServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceDenyingApp(2));
		PageHistory stored = new PageHistory();
		stored.setId(12);
		stored.setApp(2);
		stored.setTenantId("tenant-9");
		when(mapper.queryPageHistoryById(12)).thenReturn(stored);
		PageHistory request = new PageHistory();
		request.setId(12);
		request.setApp(1);

		assertThrows(TinyEngineAccessDeniedException.class, () -> service.updatePageHistoryById(request));

		verify(mapper, never()).updatePageHistoryById(any(PageHistory.class));
	}

	@Test
	void pageHistoryListRejectsHistoriesFromAnUnauthorizedApplication() {
		PageHistoryMapper mapper = mock(PageHistoryMapper.class);
		PageHistoryServiceImpl service = new PageHistoryServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		ReflectionTestUtils.setField(service, "userContext", context());
		ReflectionTestUtils.setField(service, "appRoleService", permissionServiceDenyingApp(2));
		PageHistory history = new PageHistory();
		history.setApp(2);
		history.setTenantId("tenant-9");
		when(mapper.queryAllPageHistory()).thenReturn(Collections.singletonList(history));

		assertThrows(TinyEngineAccessDeniedException.class, service::findAllPageHistory);
	}

	private AppRoleService permissionServiceDenyingApp(int deniedAppId) {
		AppRoleService service = mock(AppRoleService.class);
		doAnswer(invocation -> {
			Integer appId = invocation.getArgument(0);
			if (Integer.valueOf(deniedAppId).equals(appId)) {
				throw new TinyEngineAccessDeniedException("denied");
			}
			return null;
		}).when(service).requirePermission(any(Integer.class), any(AppPermission.class));
		return service;
	}

	private AppRoleService permissionServiceAllowingOnly(AppPermission allowedPermission) {
		AppRoleService service = mock(AppRoleService.class);
		doAnswer(invocation -> {
			AppPermission permission = invocation.getArgument(1);
			if (permission != allowedPermission) {
				throw new TinyEngineAccessDeniedException("denied");
			}
			return null;
		}).when(service).requirePermission(any(Integer.class), any(AppPermission.class));
		return service;
	}

	private TinyEngineUserContext context() {
		return () -> new TinyEngineUser("user-7", null, "tenant-9", "renter-9", null);
	}

	private App persistedAppScope(int appId) {
		App app = new App();
		app.setId(appId);
		app.setTenantId("tenant-9");
		app.setRenterId("renter-9");
		app.setSiteId("site-9");
		return app;
	}

	private void injectIfPresent(Object target, String name, Object value) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field != null) {
			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, target, value);
		}
	}
}
