package com.xiongx9527.fw.plus.tinyengine.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.tinyengine.mapper.AiModelConfigMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AiModelServiceConfigMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AppMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelImportRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelInvocationConfig;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AppAiModelSettings;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelConfig;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelBaseEntity;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelServiceConfig;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.service.AppAiModelConfigService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppAiModelConfigServiceImpl implements AppAiModelConfigService {

	private static final String SETTINGS_KEY = "aiModelSettings";

	private final TinyEngineUserContext userContext;
	private final AppRoleService appRoleService;
	private final AppMapper appMapper;
	private final AiModelServiceConfigMapper serviceMapper;
	private final AiModelConfigMapper modelMapper;

	public AppAiModelConfigServiceImpl(TinyEngineUserContext userContext, AppRoleService appRoleService,
			AppMapper appMapper, AiModelServiceConfigMapper serviceMapper, AiModelConfigMapper modelMapper) {
		this.userContext = userContext;
		this.appRoleService = appRoleService;
		this.appMapper = appMapper;
		this.serviceMapper = serviceMapper;
		this.modelMapper = modelMapper;
	}

	@Override
	public List<AiModelServiceDto> listUsable(Integer appId) {
		Scope scope = requireScope(appId, AppPermission.READ, true);
		return assemble(scope.tenantId, appId, true);
	}

	@Override
	public List<AiModelServiceDto> listManageable(Integer appId) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		return assemble(scope.tenantId, appId, false);
	}

	@Override
	public AppAiModelSettings getSettings(Integer appId) {
		Scope scope = requireScope(appId, AppPermission.READ, true);
		return settingsView(scope.app, scope.tenantId, appId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public AppAiModelSettings updateSettings(Integer appId, AppAiModelSettings request) {
		Scope scope = requireScope(appId, AppPermission.DEVELOP, true);
		if (request == null) {
			throw new IllegalArgumentException("AI model settings are required");
		}
		App lockedApp = lockApp(scope.tenantId, appId);
		AppAiModelSettings settings = AppAiModelSettings.copyOf(
				lockedApp.getExtendConfig() == null ? null : lockedApp.getExtendConfig().get(SETTINGS_KEY));
		if (request.getDefaultModel() != null) {
			settings.setDefaultModel(normalizeSelection(scope.tenantId, appId, request.getDefaultModel(), "defaultModel"));
		}
		if (request.getQuickModel() != null) {
			settings.setQuickModel(normalizeSelection(scope.tenantId, appId, request.getQuickModel(), "quickModel"));
		}
		if (request.getChatMode() != null) settings.setChatMode(request.getChatMode());
		if (request.getEnableThinking() != null) settings.setEnableThinking(request.getEnableThinking());
		settings.getAdditionalProperties().putAll(request.getAdditionalProperties());
		settings.setServices(assemble(scope.tenantId, appId, false));
		persistSettings(lockedApp, scope.tenantId, appId, settings);
		return settingsView(settings, scope.tenantId, appId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public AiModelServiceDto createService(Integer appId, AiModelServiceRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		validateServiceRequest(request, true);
		App lockedApp = lockApp(scope.tenantId, appId);
		AiModelServiceConfig service = new AiModelServiceConfig();
		applyServiceRequest(service, request);
		service.setTenantId(scope.tenantId);
		service.setAppId(appId);
		setCreateAudit(service, scope.userId);
		if (serviceMapper.insertService(service) != 1) {
			throw new IllegalStateException("Failed to create AI model service");
		}
		syncSettings(lockedApp, scope.tenantId, appId);
		return findDto(scope.tenantId, appId, service.getId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public AiModelServiceDto updateService(Integer appId, Long serviceId, AiModelServiceRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		validateId(serviceId, "serviceId");
		boolean replacement = hasText(request == null ? null : request.getApiKey());
		boolean clear = request != null && Boolean.TRUE.equals(request.getClearApiKey());
		if (replacement && clear) {
			throw new IllegalArgumentException("apiKey replacement and clearApiKey cannot both be set");
		}
		App lockedApp = lockApp(scope.tenantId, appId);
		AiModelServiceConfig service = requireService(scope.tenantId, appId, serviceId);
		applyServiceRequest(service, request);
		validateService(service, false);
		if (clear) {
			if (!Boolean.TRUE.equals(service.getAllowEmptyApiKey())) {
				throw new IllegalArgumentException("apiKey cannot be cleared unless allowEmptyApiKey is true");
			}
			service.setApiKey(null);
		}
		if (Boolean.TRUE.equals(service.getEnabled()) && !Boolean.TRUE.equals(service.getAllowEmptyApiKey())
				&& !hasText(service.getApiKey())) {
			throw new IllegalArgumentException("apiKey is required for an enabled AI model service");
		}
		setUpdateAudit(service, scope.userId);
		int changed = replacement || clear ? serviceMapper.updateService(service)
				: serviceMapper.updateKeepingApiKey(service);
		if (changed != 1) {
			throw new IllegalStateException("Failed to update AI model service");
		}
		syncSettings(lockedApp, scope.tenantId, appId);
		return findDto(scope.tenantId, appId, serviceId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public void deleteService(Integer appId, Long serviceId) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		App lockedApp = lockApp(scope.tenantId, appId);
		requireService(scope.tenantId, appId, serviceId);
		modelMapper.deleteByService(scope.tenantId, appId, serviceId);
		if (serviceMapper.deleteService(scope.tenantId, appId, serviceId) != 1) {
			throw new IllegalStateException("Failed to delete AI model service");
		}
		syncSettings(lockedApp, scope.tenantId, appId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public AiModelServiceDto addModel(Integer appId, Long serviceId, AiModelRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		validateModelRequest(request);
		App lockedApp = lockApp(scope.tenantId, appId);
		requireService(scope.tenantId, appId, serviceId);
		AiModelConfig model = new AiModelConfig();
		applyModelRequest(model, request);
		model.setTenantId(scope.tenantId);
		model.setAppId(appId);
		model.setServiceId(serviceId);
		setCreateAudit(model, scope.userId);
		if (modelMapper.insertModel(model) != 1) {
			throw new IllegalStateException("Failed to create AI model");
		}
		syncSettings(lockedApp, scope.tenantId, appId);
		return findDto(scope.tenantId, appId, serviceId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public AiModelServiceDto updateModel(Integer appId, Long serviceId, Long modelId, AiModelRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		validateModelRequest(request);
		App lockedApp = lockApp(scope.tenantId, appId);
		requireService(scope.tenantId, appId, serviceId);
		AiModelConfig model = requireModel(scope.tenantId, appId, serviceId, modelId);
		applyModelRequest(model, request);
		model.setServiceId(serviceId);
		setUpdateAudit(model, scope.userId);
		if (modelMapper.updateModel(model) != 1) {
			throw new IllegalStateException("Failed to update AI model");
		}
		syncSettings(lockedApp, scope.tenantId, appId);
		return findDto(scope.tenantId, appId, serviceId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public void deleteModel(Integer appId, Long serviceId, Long modelId) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		App lockedApp = lockApp(scope.tenantId, appId);
		requireService(scope.tenantId, appId, serviceId);
		requireModel(scope.tenantId, appId, serviceId, modelId);
		if (modelMapper.deleteModel(scope.tenantId, appId, modelId) != 1) {
			throw new IllegalStateException("Failed to delete AI model");
		}
		syncSettings(lockedApp, scope.tenantId, appId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public List<AiModelServiceDto> importLocal(Integer appId, AiModelImportRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		List<AiModelServiceDto> incoming = validateImport(request);
		App lockedApp = lockApp(scope.tenantId, appId);
		if (lockedApp.getExtendConfig() != null && lockedApp.getExtendConfig().containsKey(SETTINGS_KEY)) {
			throw new IllegalStateException("AI model configuration already exists");
		}
		if (!safeServices(serviceMapper.findServicesForUpdate(scope.tenantId, appId)).isEmpty()
				|| !safeModels(modelMapper.findModelsForUpdate(scope.tenantId, appId)).isEmpty()) {
			throw new IllegalStateException("AI model configuration already exists");
		}
		Map<String, String> apiKeys = request.getApiKeys() == null
				? Collections.<String, String>emptyMap() : request.getApiKeys();
		for (AiModelServiceDto source : incoming) {
			AiModelServiceConfig service = serviceFromDto(source, apiKeys.get(source.getServiceKey()));
			service.setTenantId(scope.tenantId);
			service.setAppId(appId);
			setCreateAudit(service, scope.userId);
			if (serviceMapper.insertService(service) != 1 || service.getId() == null) {
				throw new IllegalStateException("Failed to import AI model service");
			}
			for (AiModelDto sourceModel : safeModelDtos(source.getModels())) {
				AiModelConfig model = copyModel(sourceModel);
				model.setId(null);
				model.setTenantId(scope.tenantId);
				model.setAppId(appId);
				model.setServiceId(service.getId());
				setCreateAudit(model, scope.userId);
				if (modelMapper.insertModel(model) != 1) {
					throw new IllegalStateException("Failed to import AI model");
				}
			}
		}
		AppAiModelSettings importedSettings = AppAiModelSettings.copyOf(request.getSettings());
		if (importedSettings.getDefaultModel() != null) {
			importedSettings.setDefaultModel(normalizeSelection(scope.tenantId, appId,
					importedSettings.getDefaultModel(), "defaultModel"));
		}
		if (importedSettings.getQuickModel() != null) {
			importedSettings.setQuickModel(normalizeSelection(scope.tenantId, appId,
					importedSettings.getQuickModel(), "quickModel"));
		}
		syncSettings(lockedApp, scope.tenantId, appId, importedSettings);
		return assemble(scope.tenantId, appId, false);
	}

	@Override
	public AiModelInvocationConfig resolveInvocation(Integer appId, Long serviceId, String modelName) {
		Scope scope = requireScope(appId, AppPermission.READ, true);
		AiModelServiceConfig service = requireService(scope.tenantId, appId, serviceId);
		if (!Boolean.TRUE.equals(service.getEnabled())) {
			throw new TinyEngineAccessDeniedException("AI model service is disabled");
		}
		AiModelConfig selected = null;
		for (AiModelConfig model : safeModels(modelMapper.findModelsByService(scope.tenantId, appId, serviceId))) {
			if (model != null && modelName != null && modelName.equals(model.getName())) {
				selected = model;
				break;
			}
		}
		if (selected == null) {
			throw new IllegalArgumentException("AI model does not exist in this application service");
		}
		if (!Boolean.TRUE.equals(selected.getEnabled())) {
			throw new TinyEngineAccessDeniedException("AI model is disabled");
		}
		if (!hasText(service.getApiKey()) && !Boolean.TRUE.equals(service.getAllowEmptyApiKey())) {
			throw new IllegalStateException("AI model service requires an API key");
		}
		AiModelInvocationConfig result = new AiModelInvocationConfig();
		result.setAppId(appId);
		result.setServiceId(serviceId);
		result.setProvider(service.getProvider());
		result.setBaseUrl(service.getBaseUrl());
		result.setApiKey(service.getApiKey());
		result.setModelName(selected.getName());
		result.setSettings(parseModelSettings(selected.getSettings()));
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
	public void deleteByApp(Integer appId, String tenantId) {
		if (appId == null || !hasText(tenantId)) {
			throw new IllegalArgumentException("appId and tenantId are required");
		}
		lockApp(tenantId, appId);
		modelMapper.deleteByApp(tenantId, appId);
		serviceMapper.deleteByApp(tenantId, appId);
	}

	private App lockApp(String tenantId, Integer appId) {
		App app = appMapper.lockAppForUpdate(tenantId, appId);
		if (app == null) {
			throw new IllegalArgumentException("Application does not exist in current tenant");
		}
		return app;
	}

	private Scope requireScope(Integer appId, AppPermission permission, boolean requireEnabled) {
		if (appId == null) {
			throw new IllegalArgumentException("appId is required");
		}
		TinyEngineUser user = userContext == null ? null : userContext.getCurrentUser();
		String tenantId = user == null ? null : user.getTenantId();
		if (!hasText(tenantId)) {
			throw new TinyEngineAccessDeniedException("TinyEngine tenant is required");
		}
		appRoleService.requirePermission(appId, permission);
		App app = appMapper.queryAppById(appId, tenantId);
		if (app == null) {
			throw new IllegalArgumentException("Application does not exist in current tenant");
		}
		if (requireEnabled && Boolean.FALSE.equals(app.getEnable())) {
			throw new TinyEngineAccessDeniedException("TinyEngine application is disabled");
		}
		return new Scope(tenantId, user == null ? null : user.getUserId(), app);
	}

	private List<AiModelServiceDto> assemble(String tenantId, Integer appId, boolean enabledOnly) {
		List<AiModelServiceConfig> services = safeServices(serviceMapper.findServices(tenantId, appId));
		List<AiModelConfig> models = safeModels(modelMapper.findModels(tenantId, appId));
		Map<Long, List<AiModelDto>> byService = new HashMap<Long, List<AiModelDto>>();
		for (AiModelConfig model : models) {
			if (model == null || (enabledOnly && !Boolean.TRUE.equals(model.getEnabled()))) {
				continue;
			}
			List<AiModelDto> values = byService.get(model.getServiceId());
			if (values == null) {
				values = new ArrayList<AiModelDto>();
				byService.put(model.getServiceId(), values);
			}
			values.add(AiModelDto.from(model));
		}
		List<AiModelServiceDto> result = new ArrayList<AiModelServiceDto>();
		for (AiModelServiceConfig service : services) {
			if (service == null || (enabledOnly && !Boolean.TRUE.equals(service.getEnabled()))) {
				continue;
			}
			AiModelServiceDto dto = AiModelServiceDto.from(service);
			List<AiModelDto> serviceModels = byService.get(service.getId());
			dto.setModels(serviceModels == null ? new ArrayList<AiModelDto>() : serviceModels);
			result.add(dto);
		}
		return result;
	}

	private AiModelServiceDto findDto(String tenantId, Integer appId, Long serviceId) {
		for (AiModelServiceDto dto : assemble(tenantId, appId, false)) {
			if (serviceId.equals(dto.getId())) {
				return dto;
			}
		}
		AiModelServiceConfig service = requireService(tenantId, appId, serviceId);
		AiModelServiceDto dto = AiModelServiceDto.from(service);
		dto.setModels(toModelDtos(modelMapper.findModelsByService(tenantId, appId, serviceId)));
		return dto;
	}

	private AiModelServiceConfig requireService(String tenantId, Integer appId, Long serviceId) {
		validateId(serviceId, "serviceId");
		AiModelServiceConfig service = serviceMapper.findService(tenantId, appId, serviceId);
		if (service == null) {
			throw new IllegalArgumentException("AI model service does not exist in this application");
		}
		return service;
	}

	private AiModelConfig requireModel(String tenantId, Integer appId, Long serviceId, Long modelId) {
		validateId(modelId, "modelId");
		AiModelConfig model = modelMapper.findModel(tenantId, appId, modelId);
		if (model == null || !serviceId.equals(model.getServiceId())) {
			throw new IllegalArgumentException("AI model does not exist in this application service");
		}
		return model;
	}

	private void syncSettings(App app, String tenantId, Integer appId) {
		syncSettings(app, tenantId, appId, null);
	}

	private void syncSettings(App app, String tenantId, Integer appId, AppAiModelSettings sourceSettings) {
		Map<String, Object> copied = app.getExtendConfig() == null
				? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(app.getExtendConfig());
		AppAiModelSettings settings = AppAiModelSettings.copyOf(
				sourceSettings == null ? copied.get(SETTINGS_KEY) : sourceSettings);
		settings.setServices(assemble(tenantId, appId, false));
		persistSettings(app, tenantId, appId, settings);
	}

	private void persistSettings(App app, String tenantId, Integer appId, AppAiModelSettings settings) {
		Map<String, Object> copied = app.getExtendConfig() == null
				? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(app.getExtendConfig());
		copied.put(SETTINGS_KEY, settings);
		App update = new App();
		update.setId(appId);
		update.setTenantId(tenantId);
		update.setExtendConfig(copied);
		if (appMapper.updateAppById(update) != 1) {
			throw new IllegalStateException("Failed to update application AI model settings");
		}
	}

	private AppAiModelSettings settingsView(App app, String tenantId, Integer appId) {
		return settingsView(AppAiModelSettings.copyOf(
				app.getExtendConfig() == null ? null : app.getExtendConfig().get(SETTINGS_KEY)), tenantId, appId);
	}

	private AppAiModelSettings settingsView(AppAiModelSettings source, String tenantId, Integer appId) {
		AppAiModelSettings settings = AppAiModelSettings.copyOf(source);
		settings.setServices(assemble(tenantId, appId, true));
		return settings;
	}

	private String normalizeSelection(String tenantId, Integer appId, String selection, String fieldName) {
		if (!hasText(selection)) {
			return "";
		}
		String[] parts = selection.split("::", 2);
		if (parts.length != 2 || !hasText(parts[0]) || !hasText(parts[1])) {
			throw new IllegalArgumentException(fieldName + " must use serviceId or serviceKey with modelName");
		}
		AiModelServiceConfig selectedService = null;
		for (AiModelServiceConfig service : safeServices(serviceMapper.findServices(tenantId, appId))) {
			if (service == null) continue;
			if (parts[0].equals(service.getServiceKey()) || parts[0].equals(String.valueOf(service.getId()))) {
				selectedService = service;
				break;
			}
		}
		if (selectedService == null) {
			throw new IllegalArgumentException(fieldName + " service does not exist in this application");
		}
		if (!Boolean.TRUE.equals(selectedService.getEnabled())) {
			throw new IllegalArgumentException(fieldName + " service must be enabled");
		}
		AiModelConfig selectedModel = null;
		for (AiModelConfig model : safeModels(modelMapper.findModelsByService(tenantId, appId, selectedService.getId()))) {
			if (model != null && parts[1].equals(model.getName())) {
				selectedModel = model;
				break;
			}
		}
		if (selectedModel == null) {
			throw new IllegalArgumentException(fieldName + " model does not exist in this application service");
		}
		if (!Boolean.TRUE.equals(selectedModel.getEnabled())) {
			throw new IllegalArgumentException(fieldName + " model must be enabled");
		}
		return selectedService.getServiceKey() + "::" + selectedModel.getName();
	}

	private List<AiModelServiceDto> validateImport(AiModelImportRequest request) {
		if (request == null || request.getSettings() == null || request.getSettings().getServices() == null
				|| request.getSettings().getServices().isEmpty()) {
			throw new IllegalArgumentException("Import must contain at least one AI model service");
		}
		Map<String, Boolean> serviceKeys = new HashMap<String, Boolean>();
		for (AiModelServiceDto service : request.getSettings().getServices()) {
			if (service == null || !hasText(service.getServiceKey()) || !hasText(service.getProvider())
					|| !hasText(service.getLabel()) || !hasText(service.getBaseUrl())) {
				throw new IllegalArgumentException("Imported AI model service is invalid");
			}
			if (serviceKeys.put(service.getServiceKey(), Boolean.TRUE) != null) {
				throw new IllegalArgumentException("Imported AI model service keys must be unique");
			}
			String apiKey = request.getApiKeys() == null ? null : request.getApiKeys().get(service.getServiceKey());
			if (!hasText(apiKey) && !Boolean.TRUE.equals(service.getAllowEmptyApiKey())) {
				throw new IllegalArgumentException("Imported AI model service requires an API key");
			}
			Map<String, Boolean> modelNames = new HashMap<String, Boolean>();
			for (AiModelDto model : safeModelDtos(service.getModels())) {
				if (model == null || !hasText(model.getName()) || !hasText(model.getLabel())) {
					throw new IllegalArgumentException("Imported AI model is invalid");
				}
				if (modelNames.put(model.getName(), Boolean.TRUE) != null) {
					throw new IllegalArgumentException("Imported model names must be unique per service");
				}
			}
		}
		return request.getSettings().getServices();
	}

	private void validateServiceRequest(AiModelServiceRequest request, boolean creating) {
		if (request == null) {
			throw new IllegalArgumentException("AI model service request is required");
		}
		if (creating && Boolean.TRUE.equals(request.getClearApiKey())) {
			throw new IllegalArgumentException("clearApiKey is not valid when creating a service");
		}
		AiModelServiceConfig value = new AiModelServiceConfig();
		applyServiceRequest(value, request);
		validateService(value, creating);
	}

	private void validateService(AiModelServiceConfig service, boolean creating) {
		if (!hasText(service.getServiceKey()) || !hasText(service.getProvider()) || !hasText(service.getLabel())
				|| !hasText(service.getBaseUrl())) {
			throw new IllegalArgumentException("serviceKey, provider, label and baseUrl are required");
		}
		if (creating && !hasText(service.getApiKey()) && !Boolean.TRUE.equals(service.getAllowEmptyApiKey())) {
			throw new IllegalArgumentException("apiKey is required unless allowEmptyApiKey is true");
		}
	}

	private void validateModelRequest(AiModelRequest request) {
		if (request == null || !hasText(request.getName()) || !hasText(request.getLabel())) {
			throw new IllegalArgumentException("model name and label are required");
		}
	}

	private void applyServiceRequest(AiModelServiceConfig service, AiModelServiceRequest request) {
		if (request == null) {
			return;
		}
		if (request.getServiceKey() != null) service.setServiceKey(request.getServiceKey().trim());
		if (request.getProvider() != null) service.setProvider(request.getProvider().trim());
		if (request.getLabel() != null) service.setLabel(request.getLabel().trim());
		if (request.getBaseUrl() != null) service.setBaseUrl(request.getBaseUrl().trim());
		if (hasText(request.getApiKey())) service.setApiKey(request.getApiKey());
		if (request.getAllowEmptyApiKey() != null) service.setAllowEmptyApiKey(request.getAllowEmptyApiKey());
		if (request.getEnabled() != null) service.setEnabled(request.getEnabled());
		if (request.getSortOrder() != null) service.setSortOrder(request.getSortOrder());
		if (service.getAllowEmptyApiKey() == null) service.setAllowEmptyApiKey(Boolean.FALSE);
		if (service.getEnabled() == null) service.setEnabled(Boolean.TRUE);
		if (service.getSortOrder() == null) service.setSortOrder(Integer.valueOf(0));
	}

	private void applyModelRequest(AiModelConfig model, AiModelRequest request) {
		model.setName(request.getName().trim());
		model.setLabel(request.getLabel().trim());
		model.setCapabilities(request.getCapabilities());
		if (request.getSettings() != null) model.setSettings(normalizeModelSettings(request.getSettings()));
		model.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
		model.setSortOrder(request.getSortOrder() == null ? Integer.valueOf(0) : request.getSortOrder());
	}

	private AiModelServiceConfig serviceFromDto(AiModelServiceDto source, String apiKey) {
		AiModelServiceConfig service = new AiModelServiceConfig();
		service.setServiceKey(source.getServiceKey().trim());
		service.setProvider(source.getProvider().trim());
		service.setLabel(source.getLabel().trim());
		service.setBaseUrl(source.getBaseUrl().trim());
		service.setApiKey(hasText(apiKey) ? apiKey : null);
		service.setAllowEmptyApiKey(Boolean.TRUE.equals(source.getAllowEmptyApiKey()));
		service.setEnabled(source.getEnabled() == null ? Boolean.TRUE : source.getEnabled());
		service.setSortOrder(source.getSortOrder() == null ? Integer.valueOf(0) : source.getSortOrder());
		return service;
	}

	private AiModelConfig copyModel(AiModelDto source) {
		AiModelConfig model = new AiModelConfig();
		model.setName(source.getName().trim());
		model.setLabel(source.getLabel().trim());
		model.setCapabilities(source.getCapabilities());
		model.setSettings(normalizeModelSettings(source.getSettings()));
		model.setEnabled(source.getEnabled() == null ? Boolean.TRUE : source.getEnabled());
		model.setSortOrder(source.getSortOrder() == null ? Integer.valueOf(0) : source.getSortOrder());
		return model;
	}

	private void setCreateAudit(AiModelBaseEntity entity, String userId) {
		LocalDateTime now = LocalDateTime.now();
		entity.setCreatedBy(userId);
		entity.setLastUpdatedBy(userId);
		entity.setCreatedTime(now);
		entity.setLastUpdatedTime(now);
	}

	private void setUpdateAudit(AiModelBaseEntity entity, String userId) {
		entity.setLastUpdatedBy(userId);
		entity.setLastUpdatedTime(LocalDateTime.now());
	}

	private static void validateId(Long id, String name) {
		if (id == null || id.longValue() <= 0L) {
			throw new IllegalArgumentException(name + " is required");
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private String normalizeModelSettings(String value) {
		if (!hasText(value)) {
			return null;
		}
		try {
			JSONObject parsed = JSON.parseObject(value);
			if (parsed == null) {
				throw new IllegalArgumentException("AI model settings must be a JSON object");
			}
			return JSON.toJSONString(parsed);
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw new IllegalArgumentException("AI model settings must be a valid JSON object", ex);
		}
	}

	private Map<String, Object> parseModelSettings(String value) {
		String normalized = normalizeModelSettings(value);
		if (normalized == null) {
			return Collections.<String, Object>emptyMap();
		}
		return new LinkedHashMap<String, Object>(JSON.parseObject(normalized));
	}

	private static List<AiModelServiceConfig> safeServices(List<AiModelServiceConfig> values) {
		return values == null ? Collections.<AiModelServiceConfig>emptyList() : values;
	}

	private static List<AiModelDto> safeModelDtos(List<AiModelDto> values) {
		return values == null ? Collections.<AiModelDto>emptyList() : values;
	}

	private static List<AiModelDto> toModelDtos(List<AiModelConfig> values) {
		List<AiModelDto> result = new ArrayList<AiModelDto>();
		for (AiModelConfig model : safeModels(values)) {
			if (model != null) result.add(AiModelDto.from(model));
		}
		return result;
	}

	private static List<AiModelConfig> safeModels(List<AiModelConfig> values) {
		return values == null ? Collections.<AiModelConfig>emptyList() : values;
	}

	private static class Scope {
		private final String tenantId;
		private final String userId;
		private final App app;

		private Scope(String tenantId, String userId, App app) {
			this.tenantId = tenantId;
			this.userId = userId;
			this.app = app;
		}
	}
}
