package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerEntity;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerProvider;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.HashMap;
import java.util.Map;

/**
 * swagger 文档端点的栈无关构建逻辑，servlet 与 webflux 两条栈支线共用：
 * 解析端点路径、构建 SwaggerProvider、拼接文档 URL。
 */
public class SwaggerEndpointSupport {

	static final String DEFAULT_LOCATION = "/v2/api-docs/magic-api/swagger2.json";

	private final SwaggerConfig swaggerConfig;
	private final MagicResourceService magicResourceService;
	private final MagicAPIProperties properties;

	public SwaggerEndpointSupport(SwaggerConfig swaggerConfig,
			MagicResourceService magicResourceService,
			MagicAPIProperties properties) {
		this.swaggerConfig = swaggerConfig;
		this.magicResourceService = magicResourceService;
		this.properties = properties;
	}

	/**
	 * 解析文档端点路径，未配置时取默认值。
	 *
	 * @return 端点路径
	 */
	public String resolveLocation() {
		String location = swaggerConfig.getLocation();
		return location == null || location.isEmpty() ? DEFAULT_LOCATION : location;
	}

	/**
	 * 基于栈无关的注册表视图创建 SwaggerProvider。
	 *
	 * @param registryView registry 视图
	 * @param contextPath  应用 contextPath，无则为空串
	 * @return SwaggerProvider
	 */
	public SwaggerProvider createProvider(MagicApiRegistryView registryView, String contextPath) {
		SwaggerEntity.License license = new SwaggerEntity.License("MIT", "https://gitee.com/ssssssss-team/magic-api/blob/master/LICENSE");
		SwaggerEntity.Info info = new SwaggerEntity.Info(swaggerConfig.getDescription(), swaggerConfig.getVersion(), swaggerConfig.getTitle(), license, swaggerConfig.getConcat());

		Map<String, Object> securityDefinitionMap = new HashMap<>();
		Map<String, Object> securityMap = new HashMap<>();

		if (swaggerConfig.getBasicAuth() != null) {
			securityDefinitionMap.put(SwaggerEntity.BasicAuth.KEY_NAME, swaggerConfig.getBasicAuth());
			securityMap.put(SwaggerEntity.BasicAuth.KEY_NAME, new String[]{});
		}
		if (swaggerConfig.getApiKeyAuth() != null) {
			securityDefinitionMap.put(SwaggerEntity.ApiKeyAuth.KEY_NAME, swaggerConfig.getApiKeyAuth());
			securityMap.put(SwaggerEntity.ApiKeyAuth.KEY_NAME, new String[]{});
		}
		if (swaggerConfig.getOauth2() != null) {
			SwaggerEntity.OAuth2 oAuth2 = swaggerConfig.getOauth2();
			securityDefinitionMap.put(SwaggerEntity.OAuth2.KEY_NAME, oAuth2);

			Map<String, String> scopes = oAuth2.getScopes();
			if (scopes != null) {
				securityMap.put(SwaggerEntity.OAuth2.KEY_NAME, scopes.keySet().toArray(new String[0]));
			}
		}

		return new SwaggerProvider(
				registryView,
				magicResourceService,
				normalizeContextPath(contextPath),
				info,
				properties.isPersistenceResponseBody(),
				properties.getPrefix(),
				securityDefinitionMap,
				securityMap);
	}

	/**
	 * 拼接供 Swagger UI 分组使用的文档 URL。
	 *
	 * @param contextPath 应用 contextPath，无则为空串
	 * @return 文档完整 URL
	 */
	public String buildSwaggerUrl(String contextPath) {
		String path = normalizeContextPath(contextPath);
		String location = resolveLocation();

		if (path.endsWith("/") && location.startsWith("/")) {
			return path + location.substring(1);
		} else if (!path.endsWith("/") && !location.startsWith("/")) {
			return path + "/" + location;
		} else {
			return path + location;
		}
	}

	/**
	 * 解析 contextPath：统一将空值收敛为空串。
	 *
	 * @param configured 配置的 contextPath，可为空
	 * @return 非 null 的 contextPath
	 */
	public static String normalizeContextPath(String configured) {
		return StringUtils.defaultIfBlank(configured, "");
	}
}
