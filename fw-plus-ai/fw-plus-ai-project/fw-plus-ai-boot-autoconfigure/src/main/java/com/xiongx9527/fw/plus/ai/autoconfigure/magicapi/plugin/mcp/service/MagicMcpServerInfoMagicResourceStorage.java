package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicMcpServerInfo;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.List;
import java.util.Objects;

/**
 * {@code mcp-server} 资源类型存储：每份 JSON 定义新增一个 MCP server 实例。
 */
public class MagicMcpServerInfoMagicResourceStorage implements MagicResourceStorage<MagicMcpServerInfo> {

	private MagicResourceService magicResourceService;

	@Override
	public String folder() {
		return "mcp-server";
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<MagicMcpServerInfo> magicClass() {
		return MagicMcpServerInfo.class;
	}

	@Override
	public boolean requirePath() {
		return false;
	}

	@Override
	public boolean requiredScript() {
		return false;
	}

	@Override
	public boolean allowRoot() {
		return true;
	}

	@Override
	public String buildMappingKey(MagicMcpServerInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	@Override
	public void validate(MagicMcpServerInfo entity) {
		validateRequired(entity.getKey(), "key");
		if (!entity.getKey().matches("^[\\w.-]+$")) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.MagicMcpServerInfoMagicResourceStorage.keyPatternInvalid"));
		}
		validateRequired(entity.getEndpoint(), "endpoint");
		if (!entity.getEndpoint().startsWith("/")) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.MagicMcpServerInfoMagicResourceStorage.endpointInvalid"));
		}
		String transport = entity.getTransport();
		if (transport != null && !"http-sse".equals(transport) && !"streamable-http".equals(transport)) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.MagicMcpServerInfoMagicResourceStorage.transportUnsupported"));
		}
		validateAuthProvider(entity.getAuth());
		validateKeyUnique(entity.getKey(), entity.getId());
	}

	/**
	 * 校验认证 provider：未配置允许（未认证拒绝访问）；配置时必须是当前 classpath 可用的
	 * 内置 provider（与 Starter 同一份清单，classpath 缺失的模块不可选，也不允许保存）。
	 */
	private void validateAuthProvider(MagicApiMcpProperties.Auth auth) {
		if (auth == null || auth.getProvider() == null || auth.getProvider().trim().isEmpty()) {
			return;
		}
		String provider = auth.getProvider().trim();
		List<String> availableProviders = InstanceAuthRegistry.availableProviders(
				InstanceAuthRegistry.class.getClassLoader());
		if (!availableProviders.contains(provider)) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.MagicMcpServerInfoMagicResourceStorage.authProviderUnsupported", provider));
		}
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	@Override
	public MagicMcpServerInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, MagicMcpServerInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	@Override
	public MagicMcpServerInfo readResource(Resource resource) {
		return read(resource.read());
	}

	private void validateRequired(String value, String field) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.MagicMcpServerInfoMagicResourceStorage.fieldRequired", field));
		}
	}

	private void validateKeyUnique(String key, String currentId) {
		if (key == null || magicResourceService == null) {
			return;
		}
		boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
				.map(it -> (MagicMcpServerInfo) it)
				.filter(it -> !Objects.equals(it.getId(), currentId))
				.noneMatch(it -> Objects.equals(it.getKey(), key));
		if (!noneMatchKey) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.MagicMcpServerInfoMagicResourceStorage.keyDuplicated", key));
		}
	}
}