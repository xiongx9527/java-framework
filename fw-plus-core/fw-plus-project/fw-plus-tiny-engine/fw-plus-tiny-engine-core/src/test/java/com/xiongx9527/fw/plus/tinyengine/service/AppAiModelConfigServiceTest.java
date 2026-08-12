package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.mapper.AiModelConfigMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AiModelServiceConfigMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelImportRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelInvocationConfig;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppAiModelSettings;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelConfig;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelServiceConfig;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.service.impl.AppAiModelConfigServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppRoleCode;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppAiModelConfigServiceTest {

	@ParameterizedTest
	@EnumSource(AppRoleCode.class)
	void ownerAdminCanManageDeveloperCannotAndAllRolesCanResolve(AppRoleCode role) {
		Fixture fixture = fixture(role.getCode());
		if (AppRoleCode.APP_DEV == role) {
			doThrow(new TinyEngineAccessDeniedException("denied")).when(fixture.roleService)
					.requirePermission(Integer.valueOf(2), AppPermission.MANAGE);
			assertThatThrownBy(() -> fixture.service.deleteService(Integer.valueOf(2), Long.valueOf(10)))
					.isInstanceOf(TinyEngineAccessDeniedException.class);
			verify(fixture.serviceMapper, never()).deleteService(any(String.class), any(Integer.class), any(Long.class));
		} else {
			fixture.service.listManageable(Integer.valueOf(2));
			verify(fixture.roleService).requirePermission(Integer.valueOf(2), AppPermission.MANAGE);
		}

		stubInvocation(fixture, true, true, true);
		AiModelInvocationConfig invocation = fixture.service.resolveInvocation(
				Integer.valueOf(2), Long.valueOf(10), "gpt-4o");
		assertThat(invocation.getApiKey()).isEqualTo("secret");
		verify(fixture.roleService).requirePermission(Integer.valueOf(2), AppPermission.READ);
	}

	@Test
	void listAndResolveAlwaysUseCurrentTenantAndRequestedApp() {
		Fixture fixture = fixture("developer");
		fixture.service.listUsable(Integer.valueOf(2));

		verify(fixture.appMapper).queryAppById(Integer.valueOf(2), "tenant-9");
		verify(fixture.serviceMapper).findServices("tenant-9", Integer.valueOf(2));
		verify(fixture.modelMapper).findModels("tenant-9", Integer.valueOf(2));
		verify(fixture.serviceMapper, never()).findServices("other-tenant", Integer.valueOf(2));

		when(fixture.serviceMapper.findService("tenant-9", Integer.valueOf(2), Long.valueOf(99)))
				.thenReturn(null);
		assertThatThrownBy(() -> fixture.service.resolveInvocation(Integer.valueOf(2), Long.valueOf(99), "gpt-4o"))
				.isInstanceOf(IllegalArgumentException.class);
		verify(fixture.modelMapper, never()).findModelsByService("tenant-9", Integer.valueOf(2), Long.valueOf(99));
	}

	@Test
	void usableListFiltersDisabledServicesAndModels() {
		Fixture fixture = fixture("developer");
		AiModelServiceConfig enabledService = serviceConfig(10, true, "secret");
		AiModelServiceConfig disabledService = serviceConfig(11, false, "secret-2");
		AiModelConfig enabledModel = modelConfig(20, 10L, "enabled", true);
		AiModelConfig disabledModel = modelConfig(21, 10L, "disabled", false);
		when(fixture.serviceMapper.findServices("tenant-9", Integer.valueOf(2)))
				.thenReturn(Arrays.asList(enabledService, disabledService));
		when(fixture.modelMapper.findModels("tenant-9", Integer.valueOf(2)))
				.thenReturn(Arrays.asList(enabledModel, disabledModel));

		List<AiModelServiceDto> result = fixture.service.listUsable(Integer.valueOf(2));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo(Long.valueOf(10));
		assertThat(result.get(0).getModels()).extracting(AiModelConfig::getName).containsExactly("enabled");
	}

	@Test
	void invocationRejectsDisabledApplicationServiceOrModel() {
		Fixture disabledApp = fixture("developer");
		disabledApp.app.setEnable(Boolean.FALSE);
		stubInvocation(disabledApp, true, true, true);
		assertThatThrownBy(() -> disabledApp.service.resolveInvocation(2, 10L, "gpt-4o"))
				.isInstanceOf(TinyEngineAccessDeniedException.class);

		Fixture disabledService = fixture("developer");
		stubInvocation(disabledService, false, true, true);
		assertThatThrownBy(() -> disabledService.service.resolveInvocation(2, 10L, "gpt-4o"))
				.isInstanceOf(TinyEngineAccessDeniedException.class);

		Fixture disabledModel = fixture("developer");
		stubInvocation(disabledModel, true, false, true);
		assertThatThrownBy(() -> disabledModel.service.resolveInvocation(2, 10L, "gpt-4o"))
				.isInstanceOf(TinyEngineAccessDeniedException.class);

		Fixture missingKey = fixture("developer");
		stubInvocation(missingKey, true, true, false);
		assertThatThrownBy(() -> missingKey.service.resolveInvocation(2, 10L, "gpt-4o"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void serviceApiKeyUpdateHasKeepReplaceAndClearStates() {
		Fixture fixture = fixture("admin");
		AiModelServiceConfig stored = serviceConfig(10, true, "old-secret");
		when(fixture.serviceMapper.findService("tenant-9", 2, 10L)).thenReturn(stored);
		when(fixture.serviceMapper.updateKeepingApiKey(any(AiModelServiceConfig.class))).thenReturn(1);
		when(fixture.serviceMapper.updateService(any(AiModelServiceConfig.class))).thenReturn(1);

		AiModelServiceRequest keep = serviceRequest("openai", " ");
		fixture.service.updateService(2, 10L, keep);
		verify(fixture.serviceMapper).updateKeepingApiKey(any(AiModelServiceConfig.class));
		clearInvocations(fixture.serviceMapper);

		AiModelServiceRequest replace = serviceRequest("openai", "replacement");
		fixture.service.updateService(2, 10L, replace);
		verify(fixture.serviceMapper).updateService(org.mockito.ArgumentMatchers.argThat(
				value -> "replacement".equals(value.getApiKey())));
		clearInvocations(fixture.serviceMapper);

		AiModelServiceRequest clear = serviceRequest("openai", null);
		clear.setClearApiKey(Boolean.TRUE);
		clear.setAllowEmptyApiKey(Boolean.TRUE);
		fixture.service.updateService(2, 10L, clear);
		verify(fixture.serviceMapper).updateService(org.mockito.ArgumentMatchers.argThat(
				value -> value.getApiKey() == null));

		AiModelServiceRequest conflict = serviceRequest("openai", "replacement");
		conflict.setClearApiKey(Boolean.TRUE);
		assertThatThrownBy(() -> fixture.service.updateService(2, 10L, conflict))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void keepingBlankApiKeyRejectsFinalEnabledServiceWithoutKey() {
		Fixture allowEmptyToRequired = fixture("admin");
		AiModelServiceConfig emptyAllowed = serviceConfig(10L, true, null);
		emptyAllowed.setAllowEmptyApiKey(Boolean.TRUE);
		when(allowEmptyToRequired.serviceMapper.findService("tenant-9", 2, 10L)).thenReturn(emptyAllowed);
		AiModelServiceRequest requireKey = serviceRequest("openai", " ");
		requireKey.setAllowEmptyApiKey(Boolean.FALSE);

		assertThatThrownBy(() -> allowEmptyToRequired.service.updateService(2, 10L, requireKey))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("apiKey");
		verify(allowEmptyToRequired.serviceMapper, never()).updateKeepingApiKey(any(AiModelServiceConfig.class));

		Fixture disabledToEnabled = fixture("admin");
		AiModelServiceConfig disabledWithoutKey = serviceConfig(10L, false, null);
		disabledWithoutKey.setAllowEmptyApiKey(Boolean.FALSE);
		when(disabledToEnabled.serviceMapper.findService("tenant-9", 2, 10L)).thenReturn(disabledWithoutKey);
		AiModelServiceRequest enable = serviceRequest("openai", null);
		enable.setAllowEmptyApiKey(null);
		enable.setEnabled(Boolean.TRUE);

		assertThatThrownBy(() -> disabledToEnabled.service.updateService(2, 10L, enable))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("apiKey");
		verify(disabledToEnabled.serviceMapper, never()).updateKeepingApiKey(any(AiModelServiceConfig.class));
	}

	@Test
	void writesRejectCrossApplicationIdentifiers() {
		Fixture fixture = fixture("admin");
		when(fixture.serviceMapper.findService("tenant-9", 2, 10L)).thenReturn(null);
		assertThatThrownBy(() -> fixture.service.deleteService(2, 10L))
				.isInstanceOf(IllegalArgumentException.class);
		verify(fixture.serviceMapper, never()).deleteService("tenant-9", 2, 10L);

		when(fixture.serviceMapper.findService("tenant-9", 2, 10L)).thenReturn(serviceConfig(10, true, "secret"));
		when(fixture.modelMapper.findModel("tenant-9", 2, 20L)).thenReturn(modelConfig(20, 11L, "foreign", true));
		assertThatThrownBy(() -> fixture.service.updateModel(2, 10L, 20L, modelRequest("changed")))
				.isInstanceOf(IllegalArgumentException.class);
		verify(fixture.modelMapper, never()).updateModel(any(AiModelConfig.class));
	}

	@Test
	void importRejectsNonEmptyDatabaseBeforeWritingAnything() {
		Fixture fixture = fixture("owner");
		when(fixture.serviceMapper.findServicesForUpdate("tenant-9", 2))
				.thenReturn(Collections.singletonList(serviceConfig(10, true, "secret")));

		assertThatThrownBy(() -> fixture.service.importLocal(2, validImport()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already");
		verify(fixture.serviceMapper, never()).insertService(any(AiModelServiceConfig.class));
		verify(fixture.modelMapper, never()).insertModel(any(AiModelConfig.class));
		verify(fixture.appMapper, never()).updateAppById(any(App.class));
	}

	@Test
	void importPersistsAllRowsThenCopiesSharedSettingsWithoutDroppingOtherExtendConfigKeys() {
		Fixture fixture = fixture("owner");
		Map<String, Object> original = new LinkedHashMap<String, Object>();
		original.put("theme", "dark");
		fixture.app.setExtendConfig(original);
		when(fixture.serviceMapper.insertService(any(AiModelServiceConfig.class))).thenAnswer(invocation -> {
			AiModelServiceConfig inserted = invocation.getArgument(0);
			inserted.setId(100L);
			fixture.persistedServices.add(inserted);
			return 1;
		});
		when(fixture.modelMapper.insertModel(any(AiModelConfig.class))).thenAnswer(invocation -> {
			fixture.persistedModels.add(invocation.getArgument(0));
			return 1;
		});
		when(fixture.appMapper.updateAppById(any(App.class))).thenReturn(1);

		AiModelImportRequest importRequest = validImport();
		importRequest.getSettings().setDefaultModel("openai:gpt-4o");
		importRequest.getSettings().setQuickModel("openai:gpt-4o-mini");
		importRequest.getSettings().setChatMode("assistant");
		importRequest.getSettings().setEnableThinking(Boolean.TRUE);
		importRequest.getSettings().setAdditionalProperty("temperature", Double.valueOf(0.6D));
		List<AiModelServiceDto> result = fixture.service.importLocal(2, importRequest);

		assertThat(result).hasSize(1);
		verify(fixture.modelMapper).insertModel(org.mockito.ArgumentMatchers.argThat(
				model -> Long.valueOf(100).equals(model.getServiceId()) && "tenant-9".equals(model.getTenantId())
						&& Integer.valueOf(2).equals(model.getAppId())));
		verify(fixture.appMapper).updateAppById(org.mockito.ArgumentMatchers.argThat(update -> {
			Map<String, Object> extendConfig = update.getExtendConfig();
			if (extendConfig == original || !"dark".equals(extendConfig.get("theme"))
					|| !(extendConfig.get("aiModelSettings") instanceof AppAiModelSettings)) {
				return false;
			}
			AppAiModelSettings settings = (AppAiModelSettings) extendConfig.get("aiModelSettings");
			return "openai:gpt-4o".equals(settings.getDefaultModel())
					&& "openai:gpt-4o-mini".equals(settings.getQuickModel())
					&& "assistant".equals(settings.getChatMode())
					&& Boolean.TRUE.equals(settings.getEnableThinking())
					&& Double.valueOf(0.6D).equals(settings.getAdditionalProperties().get("temperature"));
		}));
		assertThat(original).containsOnlyKeys("theme");
	}

	@Test
	void syncingServicesPreservesApplicationAiBehaviorSettings() {
		Fixture fixture = fixture("owner");
		AppAiModelSettings existing = new AppAiModelSettings();
		existing.setDefaultModel("openai:gpt-4o");
		existing.setQuickModel("openai:gpt-4o-mini");
		existing.setChatMode("assistant");
		existing.setEnableThinking(Boolean.TRUE);
		existing.setAdditionalProperty("temperature", Double.valueOf(0.6D));
		Map<String, Object> extendConfig = new LinkedHashMap<String, Object>();
		extendConfig.put("aiModelSettings", existing);
		fixture.app.setExtendConfig(extendConfig);
		when(fixture.serviceMapper.insertService(any(AiModelServiceConfig.class))).thenAnswer(invocation -> {
			AiModelServiceConfig inserted = invocation.getArgument(0);
			inserted.setId(100L);
			fixture.persistedServices.add(inserted);
			return 1;
		});

		fixture.service.createService(2, serviceRequest("openai", "secret"));

		verify(fixture.appMapper).updateAppById(org.mockito.ArgumentMatchers.argThat(update -> {
			Object value = update.getExtendConfig().get("aiModelSettings");
			if (!(value instanceof AppAiModelSettings)) {
				return false;
			}
			AppAiModelSettings settings = (AppAiModelSettings) value;
			return "openai:gpt-4o".equals(settings.getDefaultModel())
					&& "openai:gpt-4o-mini".equals(settings.getQuickModel())
					&& "assistant".equals(settings.getChatMode())
					&& Boolean.TRUE.equals(settings.getEnableThinking())
					&& Double.valueOf(0.6D).equals(settings.getAdditionalProperties().get("temperature"))
					&& settings.getServices().size() == 1;
		}));
	}

	@Test
	void importLocksTenantApplicationBeforeCheckingDatabaseEmptiness() {
		Fixture fixture = fixture("owner");
		when(fixture.appMapper.lockAppForUpdate("tenant-9", 2)).thenReturn(fixture.app);
		when(fixture.serviceMapper.findServicesForUpdate("tenant-9", 2))
				.thenReturn(Collections.singletonList(serviceConfig(10L, true, "secret")));

		assertThatThrownBy(() -> fixture.service.importLocal(2, validImport()))
				.isInstanceOf(IllegalStateException.class);

		org.mockito.InOrder order = inOrder(fixture.appMapper, fixture.serviceMapper);
		order.verify(fixture.appMapper).lockAppForUpdate("tenant-9", 2);
		order.verify(fixture.serviceMapper).findServicesForUpdate("tenant-9", 2);
		verify(fixture.appMapper, never()).lockAppForUpdate("other-tenant", 2);
	}

	@Test
	void importMergesSettingsWithApplicationSnapshotReadAfterLock() {
		Fixture fixture = fixture("owner");
		Map<String, Object> staleConfig = new LinkedHashMap<String, Object>();
		staleConfig.put("theme", "dark");
		fixture.app.setExtendConfig(staleConfig);
		App lockedApp = new App();
		lockedApp.setId(2);
		lockedApp.setTenantId("tenant-9");
		Map<String, Object> latestConfig = new LinkedHashMap<String, Object>(staleConfig);
		latestConfig.put("concurrentSetting", "preserved");
		lockedApp.setExtendConfig(latestConfig);
		when(fixture.appMapper.lockAppForUpdate("tenant-9", 2)).thenReturn(lockedApp);
		when(fixture.serviceMapper.insertService(any(AiModelServiceConfig.class))).thenAnswer(invocation -> {
			AiModelServiceConfig inserted = invocation.getArgument(0);
			inserted.setId(100L);
			fixture.persistedServices.add(inserted);
			return 1;
		});
		when(fixture.modelMapper.insertModel(any(AiModelConfig.class))).thenAnswer(invocation -> {
			fixture.persistedModels.add(invocation.getArgument(0));
			return 1;
		});

		fixture.service.importLocal(2, validImport());

		verify(fixture.appMapper).updateAppById(org.mockito.ArgumentMatchers.argThat(update ->
				"preserved".equals(update.getExtendConfig().get("concurrentSetting"))));
	}

	@Test
	void allAiConfigurationWritesLockApplicationBeforeAccessingChildTables() {
		Fixture createService = fixture("owner");
		when(createService.serviceMapper.insertService(any(AiModelServiceConfig.class))).thenAnswer(invocation -> {
			AiModelServiceConfig inserted = invocation.getArgument(0);
			inserted.setId(10L);
			createService.persistedServices.add(inserted);
			return 1;
		});
		createService.service.createService(2, serviceRequest("openai", "secret"));
		org.mockito.InOrder createServiceOrder = inOrder(createService.appMapper, createService.serviceMapper);
		createServiceOrder.verify(createService.appMapper).lockAppForUpdate("tenant-9", 2);
		createServiceOrder.verify(createService.serviceMapper).insertService(any(AiModelServiceConfig.class));

		Fixture updateService = fixture("owner");
		when(updateService.serviceMapper.findService("tenant-9", 2, 10L))
				.thenReturn(serviceConfig(10L, true, "secret"));
		when(updateService.serviceMapper.updateKeepingApiKey(any(AiModelServiceConfig.class))).thenReturn(1);
		updateService.service.updateService(2, 10L, serviceRequest("openai", null));
		org.mockito.InOrder updateServiceOrder = inOrder(updateService.appMapper, updateService.serviceMapper);
		updateServiceOrder.verify(updateService.appMapper).lockAppForUpdate("tenant-9", 2);
		updateServiceOrder.verify(updateService.serviceMapper).findService("tenant-9", 2, 10L);

		Fixture deleteService = fixture("owner");
		when(deleteService.serviceMapper.findService("tenant-9", 2, 10L))
				.thenReturn(serviceConfig(10L, true, "secret"));
		when(deleteService.serviceMapper.deleteService("tenant-9", 2, 10L)).thenReturn(1);
		deleteService.service.deleteService(2, 10L);
		org.mockito.InOrder deleteServiceOrder = inOrder(deleteService.appMapper, deleteService.serviceMapper);
		deleteServiceOrder.verify(deleteService.appMapper).lockAppForUpdate("tenant-9", 2);
		deleteServiceOrder.verify(deleteService.serviceMapper).findService("tenant-9", 2, 10L);

		Fixture addModel = fixture("owner");
		when(addModel.serviceMapper.findService("tenant-9", 2, 10L))
				.thenReturn(serviceConfig(10L, true, "secret"));
		when(addModel.modelMapper.insertModel(any(AiModelConfig.class))).thenReturn(1);
		addModel.service.addModel(2, 10L, modelRequest("gpt-4o"));
		org.mockito.InOrder addModelOrder = inOrder(addModel.appMapper, addModel.serviceMapper);
		addModelOrder.verify(addModel.appMapper).lockAppForUpdate("tenant-9", 2);
		addModelOrder.verify(addModel.serviceMapper).findService("tenant-9", 2, 10L);

		Fixture updateModel = fixture("owner");
		when(updateModel.serviceMapper.findService("tenant-9", 2, 10L))
				.thenReturn(serviceConfig(10L, true, "secret"));
		when(updateModel.modelMapper.findModel("tenant-9", 2, 20L))
				.thenReturn(modelConfig(20L, 10L, "gpt-4o", true));
		when(updateModel.modelMapper.updateModel(any(AiModelConfig.class))).thenReturn(1);
		updateModel.service.updateModel(2, 10L, 20L, modelRequest("gpt-4o"));
		org.mockito.InOrder updateModelOrder = inOrder(updateModel.appMapper, updateModel.serviceMapper);
		updateModelOrder.verify(updateModel.appMapper).lockAppForUpdate("tenant-9", 2);
		updateModelOrder.verify(updateModel.serviceMapper).findService("tenant-9", 2, 10L);

		Fixture deleteModel = fixture("owner");
		when(deleteModel.serviceMapper.findService("tenant-9", 2, 10L))
				.thenReturn(serviceConfig(10L, true, "secret"));
		when(deleteModel.modelMapper.findModel("tenant-9", 2, 20L))
				.thenReturn(modelConfig(20L, 10L, "gpt-4o", true));
		when(deleteModel.modelMapper.deleteModel("tenant-9", 2, 20L)).thenReturn(1);
		deleteModel.service.deleteModel(2, 10L, 20L);
		org.mockito.InOrder deleteModelOrder = inOrder(deleteModel.appMapper, deleteModel.serviceMapper);
		deleteModelOrder.verify(deleteModel.appMapper).lockAppForUpdate("tenant-9", 2);
		deleteModelOrder.verify(deleteModel.serviceMapper).findService("tenant-9", 2, 10L);

		Fixture deleteByApp = fixture("owner");
		deleteByApp.service.deleteByApp(2, "tenant-9");
		org.mockito.InOrder deleteByAppOrder = inOrder(deleteByApp.appMapper, deleteByApp.modelMapper);
		deleteByAppOrder.verify(deleteByApp.appMapper).lockAppForUpdate("tenant-9", 2);
		deleteByAppOrder.verify(deleteByApp.modelMapper).deleteByApp("tenant-9", 2);
	}

	@Test
	void importAndDeleteMethodsDeclareRollbackForException() throws Exception {
		assertRollbackFor("createService", Integer.class, AiModelServiceRequest.class);
		assertRollbackFor("updateService", Integer.class, Long.class, AiModelServiceRequest.class);
		assertRollbackFor("deleteService", Integer.class, Long.class);
		assertRollbackFor("addModel", Integer.class, Long.class, AiModelRequest.class);
		assertRollbackFor("updateModel", Integer.class, Long.class, Long.class, AiModelRequest.class);
		assertRollbackFor("deleteModel", Integer.class, Long.class, Long.class);
		assertRollbackFor("importLocal", Integer.class, AiModelImportRequest.class);
		assertRollbackFor("deleteByApp", Integer.class, String.class);
	}

	private static void assertRollbackFor(String methodName, Class<?>... parameterTypes) throws Exception {
		Method method = AppAiModelConfigServiceImpl.class.getMethod(methodName, parameterTypes);
		Transactional transactional = method.getAnnotation(Transactional.class);
		assertThat(transactional).isNotNull();
		assertThat(transactional.rollbackFor()).contains(Exception.class);
		assertThat(transactional.isolation()).isEqualTo(Isolation.READ_COMMITTED);
	}

	private static Fixture fixture(String userId) {
		TinyEngineUserContext context = new TinyEngineUserContext() {
			@Override
			public TinyEngineUser getCurrentUser() {
				return new TinyEngineUser(userId, userId, "tenant-9", null, null);
			}
		};
		Fixture fixture = new Fixture();
		fixture.roleService = mock(AppRoleService.class);
		fixture.appMapper = mock(AppMapper.class);
		fixture.serviceMapper = mock(AiModelServiceConfigMapper.class);
		fixture.modelMapper = mock(AiModelConfigMapper.class);
		fixture.app = new App();
		fixture.app.setId(2);
		fixture.app.setEnable(Boolean.TRUE);
		fixture.app.setTenantId("tenant-9");
		when(fixture.appMapper.queryAppById(2, "tenant-9")).thenReturn(fixture.app);
		when(fixture.appMapper.lockAppForUpdate("tenant-9", 2)).thenReturn(fixture.app);
		fixture.persistedServices = new java.util.ArrayList<AiModelServiceConfig>();
		fixture.persistedModels = new java.util.ArrayList<AiModelConfig>();
		when(fixture.serviceMapper.findServices("tenant-9", 2)).thenAnswer(invocation -> fixture.persistedServices);
		when(fixture.serviceMapper.findServicesForUpdate("tenant-9", 2))
				.thenAnswer(invocation -> fixture.persistedServices);
		when(fixture.modelMapper.findModels("tenant-9", 2)).thenAnswer(invocation -> fixture.persistedModels);
		when(fixture.modelMapper.findModelsForUpdate("tenant-9", 2))
				.thenAnswer(invocation -> fixture.persistedModels);
		when(fixture.appMapper.updateAppById(any(App.class))).thenReturn(1);
		fixture.service = new AppAiModelConfigServiceImpl(context, fixture.roleService, fixture.appMapper,
				fixture.serviceMapper, fixture.modelMapper);
		return fixture;
	}

	private static void stubInvocation(Fixture fixture, boolean serviceEnabled, boolean modelEnabled,
			boolean hasApiKey) {
		AiModelServiceConfig service = serviceConfig(10, serviceEnabled, hasApiKey ? "secret" : null);
		service.setAllowEmptyApiKey(Boolean.FALSE);
		when(fixture.serviceMapper.findService("tenant-9", 2, 10L)).thenReturn(service);
		when(fixture.modelMapper.findModelsByService("tenant-9", 2, 10L)).thenReturn(
				Collections.singletonList(modelConfig(20, 10L, "gpt-4o", modelEnabled)));
	}

	private static AiModelServiceRequest serviceRequest(String key, String apiKey) {
		AiModelServiceRequest request = new AiModelServiceRequest();
		request.setServiceKey(key);
		request.setProvider("openai");
		request.setLabel("OpenAI");
		request.setBaseUrl("https://api.openai.com/v1");
		request.setApiKey(apiKey);
		request.setAllowEmptyApiKey(Boolean.FALSE);
		request.setEnabled(Boolean.TRUE);
		request.setSortOrder(1);
		return request;
	}

	private static AiModelRequest modelRequest(String name) {
		AiModelRequest request = new AiModelRequest();
		request.setName(name);
		request.setLabel(name);
		request.setEnabled(Boolean.TRUE);
		request.setSortOrder(1);
		return request;
	}

	private static AiModelServiceConfig serviceConfig(long id, boolean enabled, String apiKey) {
		AiModelServiceConfig service = new AiModelServiceConfig();
		service.setId(id);
		service.setAppId(2);
		service.setTenantId("tenant-9");
		service.setServiceKey("service-" + id);
		service.setProvider("openai");
		service.setLabel("OpenAI");
		service.setBaseUrl("https://api.openai.com/v1");
		service.setApiKey(apiKey);
		service.setAllowEmptyApiKey(Boolean.FALSE);
		service.setEnabled(enabled);
		service.setSortOrder(1);
		return service;
	}

	private static AiModelConfig modelConfig(long id, long serviceId, String name, boolean enabled) {
		AiModelConfig model = new AiModelConfig();
		model.setId(id);
		model.setAppId(2);
		model.setTenantId("tenant-9");
		model.setServiceId(serviceId);
		model.setName(name);
		model.setLabel(name);
		model.setEnabled(enabled);
		model.setSortOrder(1);
		return model;
	}

	private static AiModelImportRequest validImport() {
		AiModelServiceDto service = AiModelServiceDto.from(serviceConfig(7, true, null));
		service.setModels(Collections.singletonList(modelConfig(8, 7L, "gpt-4o", true)));
		AppAiModelSettings settings = new AppAiModelSettings();
		settings.setServices(Collections.singletonList(service));
		Map<String, String> apiKeys = new LinkedHashMap<String, String>();
		apiKeys.put(service.getServiceKey(), "import-secret");
		AiModelImportRequest request = new AiModelImportRequest();
		request.setSettings(settings);
		request.setApiKeys(apiKeys);
		return request;
	}

	private static class Fixture {
		private AppAiModelConfigServiceImpl service;
		private AppRoleService roleService;
		private AppMapper appMapper;
		private AiModelServiceConfigMapper serviceMapper;
		private AiModelConfigMapper modelMapper;
		private App app;
		private List<AiModelServiceConfig> persistedServices;
		private List<AiModelConfig> persistedModels;
	}
}
