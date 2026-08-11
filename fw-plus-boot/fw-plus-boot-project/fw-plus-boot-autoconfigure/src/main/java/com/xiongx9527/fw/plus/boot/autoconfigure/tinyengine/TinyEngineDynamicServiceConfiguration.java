package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.tinyengine.dynamic.mapper.ModelDataMapper;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicModelService;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicService;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl.DynamicModelServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl.DynamicServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
@ConditionalOnClass({ModelDataMapper.class, DynamicServiceImpl.class, DynamicModelServiceImpl.class})
@ConditionalOnBean(SqlSessionFactory.class)
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.mapper", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineDynamicServiceConfiguration {

	@Bean
	@ConditionalOnMissingBean(DynamicService.class)
	public DynamicService tinyEngineDynamicService(ModelDataMapper mapper, final ObjectProvider<ModelService> modelServices,
			TinyEngineUserContext userContext) {
		return new DynamicServiceImpl(mapper, new Supplier<ModelService>() {
			@Override
			public ModelService get() {
				return modelServices.getIfAvailable();
			}
		}, userContext);
	}

	@Bean
	@ConditionalOnMissingBean(DynamicModelService.class)
	public DynamicModelService tinyEngineDynamicModelService(ModelDataMapper mapper) {
		return new DynamicModelServiceImpl(mapper);
	}
}
