package com.xiongx9527.fw.plus.tinyengine.service.impl;

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
import com.xiongx9527.fw.plus.tinyengine.service.AppAiModelConfigService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.support.auth.AppPermission;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.springframework.stereotype.Service;
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
	@Transactional(rollbackFor = Exception.class)
	public AiModelServiceDto createService(Integer appId, AiModelServiceRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		validateServiceRequest(request, true);
		AiModelServiceConfig service = new AiModelServiceConfig();
		applyServiceRequest(service, request);
		service.setTenantId(scope.tenantId);
		service.setAppId(appId);
		setCreateAudit(service, scope.userId);
		if (serviceMapper.insertService(service) != 1) {
			throw new IllegalStateException("Failed to create AI model service");
		}
		syncSettings(scope.app, scope.tenantId, appId);
		return findDto(scope.tenantId, appId, longId(service.getId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AiModelServiceDto updateService(Integer appId, Long serviceId, AiModelServiceRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		validateId(serviceId, "serviceId");
		AiModelServiceConfig service = requireService(scope.tenantId, appId, serviceId);
		boolean replacement = hasText(request == null ? null : request.getApiKey());
		boolean clear = request != null && Boolean.TRUE.equals(request.getClearApiKey());
		if (replacement && clear) {
			throw new IllegalArgumentException("apiKey replacement and clearApiKey cannot both be set");
		}
		applyServiceRequest(service, request);
		validateService(service, false);
		if (clear) {
			if (!Boolean.TRUE.equals(service.getAllowEmptyApiKey())) {
				throw new IllegalArgumentException("apiKey cannot be cleared unless allowEmptyApiKey is true");
			}
			service.setApiKey(null);
		}
		setUpdateAudit(service, scope.userId);
		int changed = replacement || clear ? serviceMapper.updateService(service)
				: serviceMapper.updateKeepingApiKey(service);
		if (changed != 1) {
			throw new IllegalStateException("Failed to update AI model service");
		}
		syncSettings(scope.app, scope.tenantId, appId);
		return findDto(scope.tenantId, appId, serviceId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteService(Integer appId, Long serviceId) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		requireService(scope.tenantId, appId, serviceId);
		modelMapper.deleteByService(scope.tenantId, appId, serviceId);
		if (serviceMapper.deleteService(scope.tenantId, appId, serviceId) != 1) {
			throw new IllegalStateException("Failed to delete AI model service");
		}
		syncSettings(scope.app, scope.tenantId, appId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AiModelServiceDto addModel(Integer appId, Long serviceId, AiModelRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		requireService(scope.tenantId, appId, serviceId);
		validateModelRequest(request);
		AiModelConfig model = new AiModelConfig();
		applyModelRequest(model, request);
		model.setTenantId(scope.tenantId);
		model.setAppId(appId);
		model.setServiceId(serviceId);
		setCreateAudit(model, scope.userId);
		if (modelMapper.insertModel(model) != 1) {
			throw new IllegalStateException("Failed to create AI model");
		}
		syncSettings(scope.app, scope.tenantId, appId);
		return findDto(scope.tenantId, appId, serviceId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AiModelServiceDto updateModel(Integer appId, Long serviceId, Long modelId, AiModelRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		requireService(scope.tenantId, appId, serviceId);
		AiModelConfig model = requireModel(scope.tenantId, appId, serviceId, modelId);
		validateModelRequest(request);
		applyModelRequest(model, request);
		model.setServiceId(serviceId);
		setUpdateAudit(model, scope.userId);
		if (modelMapper.updateModel(model) != 1) {
			throw new IllegalStateException("Failed to update AI model");
		}
		syncSettings(scope.app, scope.tenantId, appId);
		return findDto(scope.tenantId, appId, serviceId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteModel(Integer appId, Long serviceId, Long modelId) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		requireService(scope.tenantId, appId, serviceId);
		requireModel(scope.tenantId, appId, serviceId, modelId);
		if (modelMapper.deleteModel(scope.tenantId, appId, modelId) != 1) {
			throw new IllegalStateException("Failed to delete AI model");
		}
		syncSettings(scope.app, scope.tenantId, appId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<AiModelServiceDto> importLocal(Integer appId, AiModelImportRequest request) {
		Scope scope = requireScope(appId, AppPermission.MANAGE, false);
		List<AiModelServiceDto> incoming = validateImport(request);
		if (!safeServices(serviceMapper.findServices(scope.tenantId, appId)).isEmpty()
				|| !safeModels(modelMapper.findModels(scope.tenantId, appId)).isEmpty()) {
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
			for (AiModelConfig sourceModel : safeModels(source.getModels())) {
				AiModelConfig model = copyModel(sourceModel);
				model.setId(null);
				model.setTenantId(scope.tenantId);
				model.setAppId(appId);
				model.setServiceId(longId(service.getId()));
				setCreateAudit(model, scope.userId);
				if (modelMapper.insertModel(model) != 1) {
					throw new IllegalStateException("Failed to import AI model");
				}
			}
		}
		syncSettings(scope.app, scope.tenantId, appId);
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
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByApp(Integer appId, String tenantId) {
		if (appId == null || !hasText(tenantId)) {
			throw new IllegalArgumentException("appId and tenantId are required");
		}
		modelMapper.deleteByApp(tenantId, appId);
		serviceMapper.deleteByApp(tenantId, appId);
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
		Map<Long, List<AiModelConfig>> byService = new HashMap<Long, List<AiModelConfig>>();
		for (AiModelConfig model : models) {
			if (model == null || (enabledOnly && !Boolean.TRUE.equals(model.getEnabled()))) {
				continue;
			}
			List<AiModelConfig> values = byService.get(model.getServiceId());
			if (values == null) {
				values = new ArrayList<AiModelConfig>();
				byService.put(model.getServiceId(), values);
			}
			values.add(model);
		}
		List<AiModelServiceDto> result = new ArrayList<AiModelServiceDto>();
		for (AiModelServiceConfig service : services) {
			if (service == null || (enabledOnly && !Boolean.TRUE.equals(service.getEnabled()))) {
				continue;
			}
			AiModelServiceDto dto = AiModelServiceDto.from(service);
			List<AiModelConfig> serviceModels = byService.get(longId(service.getId()));
			dto.setModels(serviceModels == null ? new ArrayList<AiModelConfig>() : serviceModels);
			result.add(dto);
		}
		return result;
	}

	private AiModelServiceDto findDto(String tenantId, Integer appId, Long serviceId) {
		for (AiModelServiceDto dto : assemble(tenantId, appId, false)) {
			if (serviceId.equals(longId(dto.getId()))) {
				return dto;
			}
		}
		AiModelServiceConfig service = requireService(tenantId, appId, serviceId);
		AiModelServiceDto dto = AiModelServiceDto.from(service);
		dto.setModels(safeModels(modelMapper.findModelsByService(tenantId, appId, serviceId)));
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
		Map<String, Object> copied = app.getExtendConfig() == null
				? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(app.getExtendConfig());
		AppAiModelSettings settings = new AppAiModelSettings();
		settings.setServices(assemble(tenantId, appId, false));
		copied.put(SETTINGS_KEY, settings);
		App update = new App();
		update.setId(appId);
		update.setTenantId(tenantId);
		update.setExtendConfig(copied);
		if (appMapper.updateAppById(update) != 1) {
			throw new IllegalStateException("Failed to update application AI model settings");
		}
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
			for (AiModelConfig model : safeModels(service.getModels())) {
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

	private AiModelConfig copyModel(AiModelConfig source) {
		AiModelConfig model = new AiModelConfig();
		model.setName(source.getName().trim());
		model.setLabel(source.getLabel().trim());
		model.setCapabilities(source.getCapabilities());
		model.setEnabled(source.getEnabled() == null ? Boolean.TRUE : source.getEnabled());
		model.setSortOrder(source.getSortOrder() == null ? Integer.valueOf(0) : source.getSortOrder());
		return model;
	}

	private void setCreateAudit(com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity entity, String userId) {
		LocalDateTime now = LocalDateTime.now();
		entity.setCreatedBy(userId);
		entity.setLastUpdatedBy(userId);
		entity.setCreatedTime(now);
		entity.setLastUpdatedTime(now);
	}

	private void setUpdateAudit(com.xiongx9527.fw.plus.tinyengine.common.model.BaseEntity entity, String userId) {
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

	private static Long longId(Integer id) {
		return id == null ? null : Long.valueOf(id.longValue());
	}

	private static List<AiModelServiceConfig> safeServices(List<AiModelServiceConfig> values) {
		return values == null ? Collections.<AiModelServiceConfig>emptyList() : values;
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
