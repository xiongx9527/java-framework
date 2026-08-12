package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelImportRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelInvocationConfig;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelRequest;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceRequest;

import java.util.List;

public interface AppAiModelConfigService {

	List<AiModelServiceDto> listUsable(Integer appId);
	List<AiModelServiceDto> listManageable(Integer appId);
	AiModelServiceDto createService(Integer appId, AiModelServiceRequest request);
	AiModelServiceDto updateService(Integer appId, Long serviceId, AiModelServiceRequest request);
	void deleteService(Integer appId, Long serviceId);
	AiModelServiceDto addModel(Integer appId, Long serviceId, AiModelRequest request);
	AiModelServiceDto updateModel(Integer appId, Long serviceId, Long modelId, AiModelRequest request);
	void deleteModel(Integer appId, Long serviceId, Long modelId);
	List<AiModelServiceDto> importLocal(Integer appId, AiModelImportRequest request);
	AiModelInvocationConfig resolveInvocation(Integer appId, Long serviceId, String modelName);
	void deleteByApp(Integer appId, String tenantId);
}
