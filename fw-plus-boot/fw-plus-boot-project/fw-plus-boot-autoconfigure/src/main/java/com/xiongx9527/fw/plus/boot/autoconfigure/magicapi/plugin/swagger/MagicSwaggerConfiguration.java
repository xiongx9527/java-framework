package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.web.MagicSwaggerController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.WebFluxRequestMagicDynamicRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SwaggerUiConfigParameters;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.RequestMethod;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.impl.RequestMagicDynamicRegistry;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import jakarta.servlet.ServletContext;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Magic API 的 Swagger 配置类，栈无关（servlet / webflux 双栈共用，对齐其它插件的单配置类风格）。
 *
 * <p>文档端点由统一控制器 {@link MagicSwaggerController} 提供，端点路径为
 * {@code magic-api.swagger.location}（含 {@code ${}} 占位符）：servlet 栈的 fork
 * {@code Mapping} 不解析占位符，{@code controllerRegister()} 内解析后编程注册；webflux
 * 栈由 {@code MagicAPIWebFluxControllerRegistrar} 的 swagger 分支按注解（占位符可解析）
 * 以空 base 注册。两栈端点均不带 magic-api.web 前缀。</p>
 *
 * <p>注册表视图双栈探测：webflux 引擎提供 {@code WebFluxRequestMagicDynamicRegistry}，
 * servlet 由 fork starter 提供 {@code RequestMagicDynamicRegistry}，二者均以
 * {@link MagicApiRegistryView} 收敛；控制器内懒解析，避免启动期与 {@code MagicConfiguration}
 * 循环依赖。</p>
 */
@ConditionalOnProperty(prefix = "magic-api.plugin.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass({OpenAPI.class, GroupedOpenApi.class, org.ssssssss.magicapi.swagger.SwaggerConfig.class})
@Configuration
@EnableConfigurationProperties(SwaggerConfig.class)
public class MagicSwaggerConfiguration implements MagicPluginConfiguration {
	private static final String JAVASCRIPT_FILENAME = "magic-swagger.1.0.0.iife.js";
	private static final String FRONTEND_RESOURCE = "magic-editor/plugins/" + JAVASCRIPT_FILENAME;

	private static Logger logger = LoggerFactory.getLogger(MagicSwaggerConfiguration.class);

	private final SwaggerConfig swaggerConfig;
	private final MagicResourceService magicResourceService;
	private final MagicAPIProperties properties;
	private final ObjectProvider<RequestMagicDynamicRegistry> servletRegistryProvider;
	private final ObjectProvider<WebFluxRequestMagicDynamicRegistry> webFluxRegistryProvider;
	private final ObjectProvider<ServletContext> servletContextProvider;
	private final ApplicationContext applicationContext;

	public MagicSwaggerConfiguration(SwaggerConfig swaggerConfig,
			MagicResourceService magicResourceService,
			MagicAPIProperties properties,
			ObjectProvider<RequestMagicDynamicRegistry> servletRegistryProvider,
			ObjectProvider<WebFluxRequestMagicDynamicRegistry> webFluxRegistryProvider,
			ObjectProvider<ServletContext> servletContextProvider,
			ApplicationContext applicationContext) {
		this.swaggerConfig = swaggerConfig;
		this.magicResourceService = magicResourceService;
		this.properties = properties;
		this.servletRegistryProvider = servletRegistryProvider;
		this.webFluxRegistryProvider = webFluxRegistryProvider;
		this.servletContextProvider = servletContextProvider;
		this.applicationContext = applicationContext;
	}

	/**
	 * 创建Swagger插件实例，与前端资源判定保持一致（产物缺失时降级为无前端插件）。
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
	 * servlet 栈文档端点注册：webflux 栈由 {@code MagicAPIWebFluxControllerRegistrar}
	 * 的 swagger 分支处理。fork {@code Mapping.registerController} 不解析 {@code ${}} 占位符，
	 * 故先解析 location 再编程注册；控制器内文档依赖懒初始化，此处注册不触发 registry 实例化。
	 */
	@Override
	public MagicControllerRegister controllerRegister() {
		return (mapping, configuration) -> {
			MagicSwaggerController controller = applicationContext.getBean(MagicSwaggerController.class);
			SwaggerEndpointSupport support = new SwaggerEndpointSupport(swaggerConfig, magicResourceService, properties);
			String location = support.resolveLocation();
			Method swaggerJsonMethod;
			try {
				swaggerJsonMethod = MagicSwaggerController.class.getDeclaredMethod("swaggerJson");
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException("未找到 swaggerJson 方法", e);
			}
			mapping.register(mapping.paths(location).methods(RequestMethod.GET).build(), controller, swaggerJsonMethod);
			logger.info("已注册Swagger文档端点: {}", location);
		};
	}

	/**
	 * 统一文档端点控制器：双栈共用，供 {@code controllerRegister()}（servlet）与
	 * {@code MagicAPIWebFluxControllerRegistrar}（webflux）注册。
	 */
	@Bean
	@Lazy
	public MagicSwaggerController magicSwaggerController(MagicConfiguration configuration) {
		SwaggerEndpointSupport support = new SwaggerEndpointSupport(swaggerConfig, magicResourceService, properties);
		return new MagicSwaggerController(configuration, swaggerConfig, magicResourceService, properties,
				this::resolveRegistryView, resolveContextPath());
	}

	/**
	 * 按当前栈解析文档注册表视图：webflux 引擎的注册表优先，回落 servlet 注册表。
	 * 首次调用发生在请求期（容器就绪），不触发启动期循环依赖。
	 */
	private MagicApiRegistryView resolveRegistryView() {
		WebFluxRequestMagicDynamicRegistry webFluxRegistry = webFluxRegistryProvider.getIfAvailable();
		if (webFluxRegistry != null) {
			return MagicApiRegistryView.of(webFluxRegistry);
		}
		RequestMagicDynamicRegistry servletRegistry = servletRegistryProvider.getIfAvailable();
		return servletRegistry == null ? null : MagicApiRegistryView.of(servletRegistry);
	}

	/**
	 * 解析 contextPath：优先配置项 {@code magic-api.swagger.context-path}，servlet 下缺省取
	 * {@link ServletContext}（reactive 下不存在该 bean，为空串）。
	 */
	private String resolveContextPath() {
		if (swaggerConfig.getContextPath() != null && !swaggerConfig.getContextPath().isEmpty()) {
			return swaggerConfig.getContextPath();
		}
		ServletContext servletContext = servletContextProvider.getIfAvailable();
		return servletContext == null ? "" : servletContext.getContextPath();
	}

	/**
	 * 配置Swagger UI属性，添加magic-api的Swagger文档分组（双栈共用）。
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
	 * 配置Swagger UI参数，确保magic-api的Swagger文档在UI中正确显示（双栈共用）。
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
	 * 获取Swagger文档的URL地址（供 swagger-ui 分组展示）。
	 */
	private String getSwaggerUrl() {
		SwaggerEndpointSupport support = new SwaggerEndpointSupport(swaggerConfig, magicResourceService, properties);
		return support.buildSwaggerUrl(resolveContextPath());
	}
}
