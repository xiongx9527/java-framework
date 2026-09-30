package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.MagicApiRegistryView;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.SwaggerConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.SwaggerEndpointSupport;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerEntity;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerProvider;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.function.Supplier;

/**
 * swagger 文档端点的统一控制器（servlet / webflux 双栈共用）。
 *
 * <p>端点路径为 {@code magic-api.swagger.location}（含 {@code ${}} 占位符）：webflux 栈经
 * {@code MagicAPIWebFluxMapping.registerController} 的占位符解析按注解注册；servlet 栈的
 * fork {@code Mapping} 不解析占位符，由 {@code controllerRegister()} 解析后编程注册，两栈
 * 端点均不带 magic-api.web 前缀。</p>
 *
 * <p>文档依赖懒初始化：注册表视图由 {@link Supplier} 延迟解析（servlet 的
 * {@code RequestMagicDynamicRegistry} 与 webflux 的 {@code WebFluxRequestMagicDynamicRegistry}
 * 均以 {@code AbstractMagicDynamicRegistry<ApiInfo>} 视图收敛），首次请求时容器已就绪，
 * 避免启动期与 {@code MagicConfiguration} 循环依赖。</p>
 */
public class MagicSwaggerController extends MagicController implements MagicExceptionHandler {

	private final SwaggerConfig swaggerConfig;
	private final MagicResourceService magicResourceService;
	private final MagicAPIProperties properties;
	private final Supplier<MagicApiRegistryView> registryViewSupplier;
	private final String contextPath;

	private volatile SwaggerProvider swaggerProvider;

	public MagicSwaggerController(MagicConfiguration configuration,
			SwaggerConfig swaggerConfig,
			MagicResourceService magicResourceService,
			MagicAPIProperties properties,
			Supplier<MagicApiRegistryView> registryViewSupplier,
			String contextPath) {
		super(configuration);
		this.swaggerConfig = swaggerConfig;
		this.magicResourceService = magicResourceService;
		this.properties = properties;
		this.registryViewSupplier = registryViewSupplier;
		this.contextPath = contextPath;
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
					MagicApiRegistryView registryView = registryViewSupplier.get();
					SwaggerEndpointSupport support = new SwaggerEndpointSupport(swaggerConfig,
							magicResourceService, properties);
					swaggerProvider = support.createProvider(registryView, contextPath);
				}
			}
		}
		return swaggerProvider;
	}
}
