package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@ConditionalOnClass(name = {
		"com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer",
		"org.mybatis.spring.mapper.MapperScannerConfigurer",
		"com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper"
})
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.mapper", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineMapperConfiguration {

	@Bean(name = "tinyEngineMapperScannerConfigurer")
	@ConditionalOnBean(SqlSessionFactory.class)
	@ConditionalOnMissingBean(name = "tinyEngineMapperScannerConfigurer")
	public static MapperScannerConfigurer tinyEngineMapperScannerConfigurer(TinyEngineProperties properties) {
		MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
		mapperScannerConfigurer.setBasePackage(properties.getMapper().getBasePackage());
		mapperScannerConfigurer.setProcessPropertyPlaceHolders(true);
		return mapperScannerConfigurer;
	}

	@Bean
	@ConditionalOnMissingBean(name = "tinyEngineMapperLocationsCustomizer")
	public MybatisPlusPropertiesCustomizer tinyEngineMapperLocationsCustomizer(TinyEngineProperties properties) {
		return mybatisPlusProperties -> mybatisPlusProperties.setMapperLocations(
				mergeMapperLocations(mybatisPlusProperties.getMapperLocations(), properties.getMapper().getMapperLocations()));
	}

	private static String[] mergeMapperLocations(String[] existingLocations, String[] tinyEngineLocations) {
		Set<String> mergedLocations = new LinkedHashSet<String>();
		addLocations(mergedLocations, existingLocations);
		addLocations(mergedLocations, tinyEngineLocations);
		return mergedLocations.toArray(new String[mergedLocations.size()]);
	}

	private static void addLocations(Set<String> mergedLocations, String[] locations) {
		if (locations == null) {
			return;
		}
		for (String location : locations) {
			if (StringUtils.hasText(location)) {
				mergedLocations.add(location);
			}
		}
	}
}
