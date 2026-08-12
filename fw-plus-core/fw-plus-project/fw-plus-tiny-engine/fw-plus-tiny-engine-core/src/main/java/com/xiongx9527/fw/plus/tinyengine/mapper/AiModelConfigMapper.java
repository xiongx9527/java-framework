package com.xiongx9527.fw.plus.tinyengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiModelConfigMapper extends BaseMapper<AiModelConfig> {

	List<AiModelConfig> findModels(@Param("tenantId") String tenantId, @Param("appId") Integer appId);

	List<AiModelConfig> findModelsByService(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("serviceId") Long serviceId);

	AiModelConfig findModel(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("modelId") Long modelId);

	int insertModel(AiModelConfig model);

	int updateModel(AiModelConfig model);

	int deleteModel(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("modelId") Long modelId);

	int deleteByService(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("serviceId") Long serviceId);

	int deleteByApp(@Param("tenantId") String tenantId, @Param("appId") Integer appId);
}
