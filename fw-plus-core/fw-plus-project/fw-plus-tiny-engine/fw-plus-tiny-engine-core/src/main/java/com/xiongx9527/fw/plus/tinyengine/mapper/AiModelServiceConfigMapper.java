package com.xiongx9527.fw.plus.tinyengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelServiceConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiModelServiceConfigMapper extends BaseMapper<AiModelServiceConfig> {

	List<AiModelServiceConfig> findServices(@Param("tenantId") String tenantId, @Param("appId") Integer appId);

	List<AiModelServiceConfig> findServicesForUpdate(@Param("tenantId") String tenantId,
			@Param("appId") Integer appId);

	AiModelServiceConfig findService(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("serviceId") Long serviceId);

	int insertService(AiModelServiceConfig service);

	int updateService(AiModelServiceConfig service);

	int updateKeepingApiKey(AiModelServiceConfig service);

	int deleteService(@Param("tenantId") String tenantId, @Param("appId") Integer appId,
			@Param("serviceId") Long serviceId);

	int deleteByApp(@Param("tenantId") String tenantId, @Param("appId") Integer appId);
}
