package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppRoleMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppMemberRoleRequest;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.service.impl.AppRoleServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppRoleServiceTest {

	@Test
	void permissionMatrixMatchesOwnerAdminAndDeveloperCapabilities() {
		AppRoleMapper roleMapper = mock(AppRoleMapper.class);
		AppMapper appMapper = appMapperWithApp();
		AppRoleServiceImpl service = service(roleMapper, appMapper, "user-7");

		when(roleMapper.findActiveRoleCode("tenant-9", 3, "user-7")).thenReturn("app_owner");
		for (AppPermission permission : AppPermission.values()) {
			assertDoesNotThrow(() -> service.requirePermission(3, permission));
		}

		when(roleMapper.findActiveRoleCode("tenant-9", 3, "user-7")).thenReturn("app_admin");
		assertDoesNotThrow(() -> service.requirePermission(3, AppPermission.MANAGE));
		assertDoesNotThrow(() -> service.requirePermission(3, AppPermission.PUBLISH));
		assertDoesNotThrow(() -> service.requirePermission(3, AppPermission.MANAGE_MEMBERS));
		assertThrows(TinyEngineAccessDeniedException.class,
				() -> service.requirePermission(3, AppPermission.DELETE));

		when(roleMapper.findActiveRoleCode("tenant-9", 3, "user-7")).thenReturn("app_dev");
		assertDoesNotThrow(() -> service.requirePermission(3, AppPermission.READ));
		assertDoesNotThrow(() -> service.requirePermission(3, AppPermission.DEVELOP));
		assertDoesNotThrow(() -> service.requirePermission(3, AppPermission.PREVIEW));
		assertThrows(TinyEngineAccessDeniedException.class,
				() -> service.requirePermission(3, AppPermission.MANAGE));
		assertThrows(TinyEngineAccessDeniedException.class,
				() -> service.requirePermission(3, AppPermission.PUBLISH));
	}

	@Test
	void expiredOrMissingRoleIsDeniedWhileSystemKeepsCompatibility() {
		AppRoleMapper roleMapper = mock(AppRoleMapper.class);
		AppMapper appMapper = appMapperWithApp();
		AppRoleServiceImpl realUser = service(roleMapper, appMapper, "user-7");
		assertThrows(TinyEngineAccessDeniedException.class,
				() -> realUser.requirePermission(3, AppPermission.READ));

		AppRoleServiceImpl system = service(roleMapper, appMapper, "system");
		assertDoesNotThrow(() -> system.requirePermission(3, AppPermission.DELETE));
		verify(roleMapper, never()).findActiveRoleCode("tenant-9", 3, "system");
	}

	@Test
	void disabledApplicationStillAllowsAuthorizedMemberManagement() {
		AppRoleMapper roleMapper = mock(AppRoleMapper.class);
		AppMapper appMapper = appMapperWithApp();
		App disabled = new App();
		disabled.setId(3);
		disabled.setTenantId("tenant-9");
		disabled.setEnable(false);
		when(appMapper.queryAppById(3, "tenant-9")).thenReturn(disabled);
		when(roleMapper.findActiveRoleCode("tenant-9", 3, "user-7")).thenReturn("app_admin");
		AppRoleServiceImpl service = service(roleMapper, appMapper, "user-7");

		assertDoesNotThrow(() -> service.requirePermission(3, AppPermission.MANAGE_MEMBERS));
		assertThrows(TinyEngineAccessDeniedException.class,
				() -> service.requirePermission(3, AppPermission.DEVELOP));
	}

	@Test
	void memberOperationsRejectSelfOwnerAndDuplicateAssignment() {
		AppRoleMapper roleMapper = mock(AppRoleMapper.class);
		AppMapper appMapper = appMapperWithApp();
		AppRoleServiceImpl service = service(roleMapper, appMapper, "user-7");
		when(roleMapper.findActiveRoleCode("tenant-9", 3, "user-7")).thenReturn("app_admin");
		when(roleMapper.lockApp("tenant-9", 3)).thenReturn(3);
		when(roleMapper.countUserInTenant("tenant-9", "user-8")).thenReturn(1);
		when(roleMapper.countAppRelation("tenant-9", 3, "user-8")).thenReturn(1);

		AppMemberRoleRequest self = request("user-7", "app_dev");
		assertEquals("Cannot manage your own application role",
				assertThrows(IllegalArgumentException.class, () -> service.addMember(3, self)).getMessage());
		assertEquals("Only app_admin or app_dev can be assigned",
				assertThrows(IllegalArgumentException.class,
						() -> service.addMember(3, request("user-8", "app_owner"))).getMessage());
		assertThrows(IllegalStateException.class,
				() -> service.addMember(3, request("user-8", "app_dev")));
		verify(roleMapper, never()).insertRole(any(), any(), any(), any(), any(), any());
	}

	@Test
	void memberWriteRechecksOperatorPermissionAfterApplicationLock() {
		AppRoleMapper roleMapper = mock(AppRoleMapper.class);
		AppRoleServiceImpl service = service(roleMapper, appMapperWithApp(), "user-7");
		when(roleMapper.findActiveRoleCode("tenant-9", 3, "user-7"))
				.thenReturn("app_admin")
				.thenReturn(null);
		when(roleMapper.lockApp("tenant-9", 3)).thenReturn(3);

		assertThrows(TinyEngineAccessDeniedException.class,
				() -> service.addMember(3, request("user-8", "app_dev")));
		verify(roleMapper, never()).countUserInTenant("tenant-9", "user-8");
		verify(roleMapper, never()).insertRole(any(), any(), any(), any(), any(), any());
	}

	@Test
	void ownerCreationLocksAppAndRefusesASecondOwner() {
		AppRoleMapper roleMapper = mock(AppRoleMapper.class);
		AppRoleServiceImpl service = service(roleMapper, appMapperWithApp(), "user-7");
		when(roleMapper.lockApp("tenant-9", 3)).thenReturn(3);
		when(roleMapper.countOwner("tenant-9", 3)).thenReturn(1);

		assertThrows(IllegalStateException.class, () -> service.createOwner(3, "tenant-9", "user-7"));
		verify(roleMapper, never()).insertRole(any(), any(), any(), eq("app_owner"), any(), any());
	}

	private static AppMapper appMapperWithApp() {
		AppMapper mapper = mock(AppMapper.class);
		App app = new App();
		app.setId(3);
		app.setTenantId("tenant-9");
		when(mapper.queryAppById(3, "tenant-9")).thenReturn(app);
		return mapper;
	}

	private static AppRoleServiceImpl service(AppRoleMapper roleMapper, AppMapper appMapper, String userId) {
		TinyEngineUserContext context = () -> new TinyEngineUser(userId, userId, "tenant-9", "renter-9", 1);
		return new AppRoleServiceImpl(roleMapper, appMapper, context);
	}

	private static AppMemberRoleRequest request(String userId, String roleCode) {
		AppMemberRoleRequest request = new AppMemberRoleRequest();
		request.setUserId(userId);
		request.setRoleCode(roleCode);
		return request;
	}
}
