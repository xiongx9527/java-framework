package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.SwaggerConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.SwaggerEndpointSupport;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Magic API 的 Swagger 配置类（webflux 栈支线）。
 * <p>与 servlet 支线的 {@code MagicSwaggerConfiguration} 条件互斥：仅在 reactive 应用下装配。
 * 插件描述（{@link #plugin()}）经 Spring 收集进 webflux ControllerRegistrar 的
 * {@code List<MagicPluginConfiguration>}，由 {@code registerPlugin("swagger")} 注册文档端点；
 * contextPath 取配置 {@code magic-api.swagger.context-path}（servlet 支线取 ServletContext）。
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Configuration
@EnableConfigurationProperties(SwaggerConfig.class)
@ConditionalOnClass({OpenAPI.class, GroupedOpenApi.class, org.ssssssss.magicapi.swagger.SwaggerConfig.class})
public class MagicSwaggerWebFluxConfiguration implements MagicPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-swagger.1.0.0.iife.js";
	private static final String FRONTEND_RESOURCE = "magic-editor/plugins/" + JAVASCRIPT_FILENAME;

	private final SwaggerConfig swaggerConfig;
	private final MagicResourceService magicResourceService;
	private final MagicAPIProperties properties;

	public MagicSwaggerWebFluxConfiguration(SwaggerConfig swaggerConfig,
			MagicResourceService magicResourceService,
			MagicAPIProperties properties) {
		this.swaggerConfig = swaggerConfig;
		this.magicResourceService = magicResourceService;
		this.properties = properties;
	}

	/**
	 * 创建Swagger插件实例，与 servlet 支线保持一致的前端资源判定。
	 *
	 * @return Plugin 插件实例
	 */
	@Override
	public Plugin plugin() {
		if (MagicSwaggerWebFluxConfiguration.class.getClassLoader().getResource(FRONTEND_RESOURCE) == null) {
			return new Plugin("swagger");
		}
		return new Plugin("swagger", "MagicSwagger", JAVASCRIPT_FILENAME);
	}

	/**
	 * 配置Swagger UI属性，添加magic-api的Swagger文档分组（与 servlet 支线等价）。
	 */
	@Bean
	@Primary
	public SwaggerUiConfigProperties magicSwaggerUiConfigProperties(SwaggerUiConfigProperties swaggerUiConfigProperties, SpringDocConfigProperties springDocConfigProperties) {
		Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = swaggerUiConfigProperties.getUrls();
		if (urls == null) {
			urls = new HashSet<>();
			AbstractSwaggerUiConfigProperties.SwaggerUrl url = new AbstractSwaggerUiConfigProperties.SwaggerUrl("default", springDocConfigProperties.getApiDocs().getPath(), null);
			urls.add(url);
		}
		urls.add(new AbstractSwaggerUiConfigProperties.SwaggerUrl(swaggerConfig.getName(), buildSwaggerUrl(), null));
		swaggerUiConfigProperties.setUrls(urls);
		return swaggerUiConfigProperties;
	}

	/**
	 * 配置Swagger UI参数，确保magic-api的Swagger文档在UI中正确显示（与 servlet 支线等价）。
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
				urls.add(new SwaggerUrl(swaggerConfig.getName(), buildSwaggerUrl(), null));
				params.put("urls", urls);
				return params;
			}
		};
	}

	private String buildSwaggerUrl() {
		return new SwaggerEndpointSupport(swaggerConfig, magicResourceService, properties)
				.buildSwaggerUrl(swaggerConfig.getContextPath());
	}
}
