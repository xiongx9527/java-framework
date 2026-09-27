package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.MagicApiRegistryView;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.SwaggerConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.SwaggerEndpointSupport;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerEntity;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux.WebFluxRequestMagicDynamicRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * swagger 文档端点的 webflux 栈支线控制器。
 * <p>由 {@code MagicAPIWebFluxControllerRegistrar} 以 POJO 控制器方式注册进响应式
 * {@code RequestMappingHandlerMapping}（base 为空串，与 servlet 支线一致，不加
 * {@code /magic-api/web} 前缀）；文档生成复用 servlet 支线相同的
 * {@link SwaggerEndpointSupport} / {@link SwaggerProvider}。
 */
public class MagicSwaggerWebFluxController {

	private final SwaggerConfig swaggerConfig;
	private final MagicResourceService magicResourceService;
	private final MagicAPIProperties properties;
	private final ApplicationContext applicationContext;

	private volatile SwaggerProvider swaggerProvider;

	public MagicSwaggerWebFluxController(SwaggerConfig swaggerConfig,
			MagicResourceService magicResourceService,
			MagicAPIProperties properties,
			ApplicationContext applicationContext) {
		this.swaggerConfig = swaggerConfig;
		this.magicResourceService = magicResourceService;
		this.properties = properties;
		this.applicationContext = applicationContext;
	}

	/**
	 * 输出 Swagger 2.0 文档 JSON，路径取 {@code magic-api.swagger.location}。
	 *
	 * @return Swagger 文档实体
	 */
	@GetMapping("${magic-api.swagger.location:/v2/api-docs/magic-api/swagger2.json}")
	@ResponseBody
	public SwaggerEntity swaggerJson() {
		return provider().swaggerJson();
	}

	private SwaggerProvider provider() {
		if (swaggerProvider == null) {
			synchronized (this) {
				if (swaggerProvider == null) {
					SwaggerEndpointSupport support = new SwaggerEndpointSupport(swaggerConfig, magicResourceService, properties);
					// 接口注册表为 ApiInfo 类型；容器中另有 function/dataSource 等 registry，按具体类型取用
					WebFluxRequestMagicDynamicRegistry registry = applicationContext.getBean(WebFluxRequestMagicDynamicRegistry.class);
					swaggerProvider = support.createProvider(MagicApiRegistryView.of(registry), swaggerConfig.getContextPath());
				}
			}
		}
		return swaggerProvider;
	}
}
