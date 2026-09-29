package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * {@code langchain4j-content-retriever} 资源类型存储：每份 JSON 定义新增一个 ContentRetriever 实例。
 */
public class Langchain4jContentRetrieverMagicResourceStorage
		implements MagicResourceStorage<Langchain4jContentRetrieverInfo> {

	private final Set<String> providers;

	private MagicResourceService magicResourceService;

	public Langchain4jContentRetrieverMagicResourceStorage() {
		this(Collections.<String>emptySet());
	}

	public Langchain4jContentRetrieverMagicResourceStorage(Set<String> providers) {
		this.providers = providers;
	}

	@Override
	public String folder() {
		return Langchain4jContentRetrieverInfo.TYPE;
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<Langchain4jContentRetrieverInfo> magicClass() {
		return Langchain4jContentRetrieverInfo.class;
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
	public String buildMappingKey(Langchain4jContentRetrieverInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	@Override
	public void validate(Langchain4jContentRetrieverInfo entity) {
		validateRequired(entity.getKey(), "key");
		if (!entity.getKey().matches("^[\\w.-]+$")) {
			throw new IllegalArgumentException("key只能包含字母、数字、下划线、点与中划线");
		}
		validateRequired(entity.getProvider(), "provider");
		if (!providers.contains(entity.getProvider())) {
			throw new IllegalArgumentException(String.format(
					"provider '%s' 不受支持，可用 provider: %s", entity.getProvider(), providers));
		}
		validateKeyUnique(entity);
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	@Override
	public Langchain4jContentRetrieverInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, Langchain4jContentRetrieverInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	@Override
	public Langchain4jContentRetrieverInfo readResource(Resource resource) {
		return read(resource.read());
	}

	private void validateRequired(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + "不能为空");
		}
	}

	private void validateKeyUnique(Langchain4jContentRetrieverInfo entity) {
		if (magicResourceService == null) {
			return;
		}
		boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
				.filter(it -> it instanceof Langchain4jContentRetrieverInfo)
				.map(it -> (Langchain4jContentRetrieverInfo) it)
				.filter(it -> !Objects.equals(it.getId(), entity.getId()))
				.noneMatch(it -> Objects.equals(it.getKey(), entity.getKey()));
		if (!noneMatchKey) {
			throw new IllegalArgumentException(String.format("key '%s' 已存在，请使用其他 key", entity.getKey()));
		}
	}
}
