package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerProvider;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.impl.RequestMagicDynamicRegistry;
import org.ssssssss.magicapi.utils.Mapping;

import jakarta.servlet.ServletContext;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Magic API的Swagger配置类，用于集成Swagger文档生成功能
 * 该配置类实现了Swagger端点的动态注册以及Swagger UI的配置
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Configuration
@EnableConfigurationProperties(SwaggerConfig.class)
//org.ssssssss.magicapi.swagger
@ConditionalOnClass({OpenAPI.class, GroupedOpenApi.class,org.ssssssss.magicapi.swagger.SwaggerConfig.class})
public class MagicSwaggerConfiguration implements MagicPluginConfiguration {
	private static final String JAVASCRIPT_FILENAME = "magic-swagger.1.0.0.iife.js";
	private static final String FRONTEND_RESOURCE = "magic-editor/plugins/" + JAVASCRIPT_FILENAME;

	private final SwaggerConfig swaggerConfig;
	private final MagicResourceService magicResourceService;
	private final ServletContext servletContext;
	private final ApplicationContext applicationContext;
	private final MagicAPIProperties properties;

	private boolean registered = false;

	private static Logger logger = LoggerFactory.getLogger(MagicSwaggerConfiguration.class);

	/**
	 * 构造函数，初始化Swagger配置所需的各种依赖
	 * 
	 * @param swaggerConfig Swagger配置信息
	 * @param magicResourceService Magic API资源服务
	 * @param servletContext Servlet上下文
	 * @param applicationContext Spring应用上下文
	 * @param properties Magic API属性配置
	 */
	public MagicSwaggerConfiguration(SwaggerConfig swaggerConfig, 
			MagicResourceService magicResourceService, 
			ServletContext servletContext,
			ApplicationContext applicationContext,
			MagicAPIProperties properties) {
		this.swaggerConfig = swaggerConfig;
		this.magicResourceService = magicResourceService;
		this.servletContext = servletContext;
		this.applicationContext = applicationContext;
		this.properties = properties;
	}

	/**
	 * 创建Swagger插件实例
	 * 
	 * @return Plugin 插件实例
	 */
	@Override
	public Plugin plugin() {
		if (MagicSwaggerConfiguration.class.getClassLoader().getResource(FRONTEND_RESOURCE) == null) {
			return new Plugin("swagger");
		}
		return new Plugin("swagger", "MagicSwagger", JAVASCRIPT_FILENAME);
	}

	/**
	 * 创建Swagger注册监听器Bean
	 * 
	 * @return SwaggerRegistrationListener Swagger注册监听器
	 */
	@Bean
	public SwaggerRegistrationListener swaggerRegistrationListener() {
		return new SwaggerRegistrationListener();
	}

	/**
	 * 内部方法，用于实际执行Swagger端点的注册过程
	 * 包括创建SwaggerProvider、构建文档信息、注册端点等操作
	 */
	private void registerSwaggerEndpointInternal() {
		if (registered) {
			return;
		}

		try {
			// 获取端点路径（与 webflux 支线共享解析逻辑）
			SwaggerEndpointSupport support = new SwaggerEndpointSupport(swaggerConfig, magicResourceService, properties);
			String location = support.resolveLocation();
			logger.info("正在注册Swagger端点: {}", location);
			// 明确指定使用requestMappingHandlerMapping这个Bean名称
			//RequestMappingHandlerMapping requestMappingHandlerMapping =
			//	applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
			RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext.getBeansOfType(RequestMappingHandlerMapping.class).values().stream().findFirst().get();
			// 延迟获取依赖以避免循环依赖
			RequestMagicDynamicRegistry registry = applicationContext.getBean(RequestMagicDynamicRegistry.class);

			// 使用Mapping工具注册端点
			Mapping mapping = Mapping.create(requestMappingHandlerMapping);
			RequestMappingInfo requestMappingInfo = mapping.paths(location).build();

			// 创建SwaggerProvider实例（栈无关构建，contextPath 取 ServletContext）
			SwaggerProvider swaggerProvider = support.createProvider(MagicApiRegistryView.of(registry), servletContext.getContextPath());

			// 获取swaggerJson方法
			Method swaggerJsonMethod = SwaggerProvider.class.getDeclaredMethod("swaggerJson");
			swaggerJsonMethod.setAccessible(true);

			// 注册端点
			mapping.register(requestMappingInfo, swaggerProvider, swaggerJsonMethod);
			registered = true;
			logger.info("成功注册Swagger端点: {}", location);
		} catch (Exception e) {
			logger.error("注册Swagger接口失败", e);
		}
	}

	/**
	 * 配置Swagger UI属性，添加magic-api的Swagger文档分组
	 * 
	 * @param swaggerUiConfigProperties Swagger UI配置属性
	 * @param springDocConfigProperties SpringDoc配置属性
	 * @return SwaggerUiConfigProperties 更新后的Swagger UI配置属性
	 */
	@Bean
	@Primary
	public SwaggerUiConfigProperties magicSwaggerUiConfigProperties(SwaggerUiConfigProperties swaggerUiConfigProperties, SpringDocConfigProperties springDocConfigProperties){
		Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = swaggerUiConfigProperties.getUrls();
		if (urls == null){
			urls = new HashSet<>();
			AbstractSwaggerUiConfigProperties.SwaggerUrl url = new AbstractSwaggerUiConfigProperties.SwaggerUrl("default", springDocConfigProperties.getApiDocs().getPath(), null);
			urls.add(url);
		}
		urls.add(new AbstractSwaggerUiConfigProperties.SwaggerUrl(swaggerConfig.getName(), getSwaggerUrl(), null));
		swaggerUiConfigProperties.setUrls(urls);
		return swaggerUiConfigProperties;
	}

	/**
	 * 配置Swagger UI参数，确保magic-api的Swagger文档在UI中正确显示
	 * 
	 * @param swaggerUiConfigProperties Swagger UI配置属性
	 * @return SwaggerUiConfigParameters 更新后的Swagger UI参数配置
	 */
	@Bean
	@Primary
	@Lazy
	public SwaggerUiConfigParameters magicSwaggerUiConfigParameters(SwaggerUiConfigProperties swaggerUiConfigProperties) {
		return new SwaggerUiConfigParameters(swaggerUiConfigProperties) {
			@Override
			public Map<String, Object> getConfigParameters() {
				Map<String, Object> params = super.getConfigParameters();
				Set<SwaggerUrl> urls = (Set<SwaggerUrl>) params.get("urls");
				if (urls == null) {
					urls = new HashSet<>();
					SwaggerUrl url = new SwaggerUrl("default", (String) params.remove("url"), null);
					urls.add(url);
				} else {
					urls = new HashSet<>(urls);
				}
				urls.add(new SwaggerUrl(swaggerConfig.getName(), getSwaggerUrl(), null));
				params.put("urls", urls);
				return params;
			}
		};
	}

	/**
	 * 获取Swagger文档的URL地址
	 * 
	 * @return String Swagger文档的完整URL地址
	 */
	private String getSwaggerUrl() {
		SwaggerEndpointSupport support = new SwaggerEndpointSupport(swaggerConfig, magicResourceService, properties);
		return support.buildSwaggerUrl(servletContext.getContextPath());
	}
	
	/**
	 * Swagger注册监听器，监听应用启动完成事件，在应用完全启动后注册Swagger端点
	 */
	private class SwaggerRegistrationListener implements ApplicationListener<ApplicationReadyEvent> {
		/**
		 * 处理应用启动完成事件
		 * 
		 * @param event ApplicationReadyEvent 应用启动完成事件
		 */
		@Override
		public void onApplicationEvent(ApplicationReadyEvent event) {
			if (event.getApplicationContext() == applicationContext) {
				registerSwaggerEndpointInternal();
			}
		}
	}
}
