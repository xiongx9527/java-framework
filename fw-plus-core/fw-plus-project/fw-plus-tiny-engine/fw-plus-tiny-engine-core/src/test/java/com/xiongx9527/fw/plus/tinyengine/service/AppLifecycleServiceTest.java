package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.mapper.AppExtensionMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.PageMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Platform;
import com.xiongx9527.fw.plus.tinyengine.service.impl.AppExtensionServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.impl.AppServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppLifecycleServiceTest {

	@Test
	void saveAndPublishUpdateLifecycleFields() {
		AppMapper appMapper = mock(AppMapper.class);
		AppServiceImpl service = service(appMapper, mock(PageMapper.class));
		App stored = new App();
		stored.setId(Integer.valueOf(1));
		stored.setTenantId("tenant-9");
		when(appMapper.queryAppById(Integer.valueOf(1), "tenant-9")).thenReturn(stored);
		when(appMapper.updateAppById(org.mockito.ArgumentMatchers.any(App.class))).thenReturn(Integer.valueOf(1));

		Map<String, Object> saveRequest = new LinkedHashMap<String, Object>();
		saveRequest.put("version", "1.2.3");
		service.saveApp(Integer.valueOf(1), saveRequest);

		Map<String, Object> publishRequest = new LinkedHashMap<String, Object>();
		publishRequest.put("branch", "release/1.2");
		service.publishApp(Integer.valueOf(1), publishRequest);

		ArgumentCaptor<App> captor = ArgumentCaptor.forClass(App.class);
		verify(appMapper, org.mockito.Mockito.times(2)).updateAppById(captor.capture());
		assertThat(captor.getAllValues().get(0).getLatest()).isEqualTo("1.2.3");
		assertThat(captor.getAllValues().get(1).getIsPublish()).isTrue();
		assertThat(captor.getAllValues().get(1).getBranch()).isEqualTo("release/1.2");
	}

	@Test
	void updateIgnoresRequestTenantAndUsesCurrentTenant() {
		AppMapper appMapper = mock(AppMapper.class);
		AppServiceImpl service = service(appMapper, mock(PageMapper.class));
		App stored = new App();
		stored.setId(Integer.valueOf(1));
		stored.setTenantId("tenant-9");
		when(appMapper.queryAppById(Integer.valueOf(1), "tenant-9")).thenReturn(stored);
		when(appMapper.updateAppById(org.mockito.ArgumentMatchers.any(App.class))).thenReturn(Integer.valueOf(1));
		App malicious = new App();
		malicious.setId(Integer.valueOf(1));
		malicious.setTenantId("attacker-tenant");

		service.updateAppById(malicious);

		ArgumentCaptor<App> captor = ArgumentCaptor.forClass(App.class);
		verify(appMapper, times(2)).queryAppById(Integer.valueOf(1), "tenant-9");
		verify(appMapper, never()).queryAppById(Integer.valueOf(1), "attacker-tenant");
		verify(appMapper).updateAppById(captor.capture());
		assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-9");
	}

	@Test
	void deleteAppRemovesAssociatedPagesBeforeApplication() {
		AppMapper appMapper = mock(AppMapper.class);
		PageMapper pageMapper = mock(PageMapper.class);
		AppServiceImpl service = service(appMapper, pageMapper);
		App stored = new App();
		stored.setId(Integer.valueOf(1));
		Page first = new Page();
		first.setId(Integer.valueOf(10));
		Page second = new Page();
		second.setId(Integer.valueOf(11));
		when(appMapper.queryAppById(Integer.valueOf(1), "tenant-9")).thenReturn(stored);
		when(pageMapper.queryPageByApp(Integer.valueOf(1))).thenReturn(Arrays.asList(first, second));
		when(appMapper.deleteAppById(Integer.valueOf(1), "tenant-9")).thenReturn(Integer.valueOf(1));

		assertThat(service.deleteAppById(Integer.valueOf(1))).isSameAs(stored);

		InOrder order = inOrder(pageMapper, appMapper);
		order.verify(pageMapper).deletePageById(Integer.valueOf(10));
		order.verify(pageMapper).deletePageById(Integer.valueOf(11));
		order.verify(appMapper).deleteAppById(Integer.valueOf(1), "tenant-9");
	}

	@Test
	void createAppUsesCurrentUserAuditFields() {
		AppMapper appMapper = mock(AppMapper.class);
		AppServiceImpl service = service(appMapper, mock(PageMapper.class));
		when(appMapper.queryAppByCondition(org.mockito.ArgumentMatchers.any(App.class)))
				.thenReturn(Collections.<App>emptyList());
		when(appMapper.createApp(org.mockito.ArgumentMatchers.any(App.class))).thenReturn(Integer.valueOf(1));
		App app = new App();
		app.setName("example");
		app.setPlatformId(Integer.valueOf(0));

		service.createApp(app);

		ArgumentCaptor<App> captor = ArgumentCaptor.forClass(App.class);
		verify(appMapper).createApp(captor.capture());
		assertThat(captor.getValue().getTenantId()).isEqualTo("tenant-9");
		assertThat(captor.getValue().getCreatedBy()).isEqualTo("user-7");
		assertThat(captor.getValue().getLastUpdatedBy()).isEqualTo("user-7");
		assertThat(captor.getValue().getCreatedTime()).isNotNull();
		assertThat(captor.getValue().getLastUpdatedTime()).isNotNull();
	}

	@Test
	void createAppUsesDefaultPlatformWhenRequestOmitsPlatform() {
		AppMapper appMapper = mock(AppMapper.class);
		PlatformService platformService = mock(PlatformService.class);
		AppServiceImpl service = service(appMapper, mock(PageMapper.class));
		ReflectionTestUtils.setField(service, "platformService", platformService);
		when(appMapper.queryAppByCondition(org.mockito.ArgumentMatchers.any(App.class)))
				.thenReturn(Collections.<App>emptyList());
		when(appMapper.createApp(org.mockito.ArgumentMatchers.any(App.class))).thenReturn(Integer.valueOf(1));
		Platform otherPlatform = new Platform();
		otherPlatform.setId(Integer.valueOf(7));
		otherPlatform.setIsDefault(Boolean.FALSE);
		otherPlatform.setLatestHistoryId(Integer.valueOf(70));
		Platform defaultPlatform = new Platform();
		defaultPlatform.setId(Integer.valueOf(1));
		defaultPlatform.setIsDefault(Boolean.TRUE);
		defaultPlatform.setLatestHistoryId(Integer.valueOf(9));
		when(platformService.queryAllPlatform()).thenReturn(Arrays.asList(otherPlatform, defaultPlatform));
		App app = new App();
		app.setName("example");

		service.createApp(app);

		ArgumentCaptor<App> captor = ArgumentCaptor.forClass(App.class);
		verify(appMapper).createApp(captor.capture());
		assertThat(captor.getValue().getPlatformId()).isEqualTo(Integer.valueOf(1));
		assertThat(captor.getValue().getPlatformHistoryId()).isEqualTo("9");
	}

	@Test
	void extensionCreateAndUpdatePreserveAppCategoryBoundary() {
		AppExtensionMapper mapper = mock(AppExtensionMapper.class);
		AppExtensionServiceImpl service = new AppExtensionServiceImpl();
		ReflectionTestUtils.setField(service, "baseMapper", mapper);
		AppExtension extension = extension(Integer.valueOf(1), "utils", "formatDate");
		when(mapper.queryAppExtensionByCondition(extension)).thenReturn(Collections.<AppExtension>emptyList());
		when(mapper.createAppExtension(extension)).thenReturn(Integer.valueOf(1));
		service.createAppExtension(extension);

		AppExtension stored = extension(Integer.valueOf(1), "utils", "formatDate");
		stored.setId(Integer.valueOf(8));
		AppExtension update = new AppExtension();
		update.setId(Integer.valueOf(8));
		update.setName("formatDateTime");
		when(mapper.queryAppExtensionById(Integer.valueOf(8))).thenReturn(stored, update);
		when(mapper.updateAppExtensionById(update)).thenReturn(Integer.valueOf(1));

		service.updateAppExtensionById(update);

		assertThat(update.getApp()).isEqualTo(Integer.valueOf(1));
		assertThat(update.getCategory()).isEqualTo("utils");
	}

	private static AppServiceImpl service(AppMapper appMapper, PageMapper pageMapper) {
		TinyEngineUserContext context = new TinyEngineUserContext() {
			@Override
			public TinyEngineUser getCurrentUser() {
				return new TinyEngineUser("user-7", "tester", "tenant-9", null, null);
			}
		};
		AppServiceImpl service = new AppServiceImpl(context);
		ReflectionTestUtils.setField(service, "baseMapper", appMapper);
		ReflectionTestUtils.setField(service, "pageMapper", pageMapper);
		return service;
	}

	private static AppExtension extension(Integer app, String category, String name) {
		AppExtension extension = new AppExtension();
		extension.setApp(app);
		extension.setCategory(category);
		extension.setName(name);
		return extension;
	}
}
