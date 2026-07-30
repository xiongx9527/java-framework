package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureAfter(name = {
		"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
		"org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
		"com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.tinyengine.service.ResourceService")
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine", name = "enabled", havingValue = "true")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(TinyEngineProperties.class)
@Import({TinyEngineCoreConfiguration.class, TinyEngineServiceConfiguration.class, TinyEngineMapperConfiguration.class,
		TinyEngineResourceConfiguration.class, TinyEngineWebConfiguration.class})
public class TinyEngineAutoConfiguration {
}
