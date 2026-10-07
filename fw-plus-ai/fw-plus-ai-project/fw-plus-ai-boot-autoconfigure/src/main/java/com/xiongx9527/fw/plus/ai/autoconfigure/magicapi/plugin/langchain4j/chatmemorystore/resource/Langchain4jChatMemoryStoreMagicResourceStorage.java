package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.Langchain4jChatMemoryStoreInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

/**
 * {@code langchain4j-chat-memory-store} 资源类型存储：每份 JSON 定义新增一个 ChatMemoryStore 实例。
 */
public class Langchain4jChatMemoryStoreMagicResourceStorage
		implements MagicResourceStorage<Langchain4jChatMemoryStoreInfo> {

	private MagicResourceService magicResourceService;

	@Override
	public String folder() {
		return "langchain4j-chat-memory-store";
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<Langchain4jChatMemoryStoreInfo> magicClass() {
		return Langchain4jChatMemoryStoreInfo.class;
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
	public String buildMappingKey(Langchain4jChatMemoryStoreInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	@Override
	public void validate(Langchain4jChatMemoryStoreInfo entity) {
		validateRequired(entity.getKey(), "key");
		if (!entity.getKey().matches("^[\\w.-]+$")) {
			throw new IllegalArgumentException("key只能包含字母、数字、下划线、点与中划线");
		}
		String type = entity.getType() == null ? "" : entity.getType().trim().toLowerCase();
		switch (type) {
			case ChatMemoryStoreProperties.TYPE_IN_MEMORY:
			case ChatMemoryStoreProperties.TYPE_JDBC:
			case ChatMemoryStoreProperties.TYPE_REDIS:
			case ChatMemoryStoreProperties.TYPE_MONGODB:
			case ChatMemoryStoreProperties.TYPE_ELASTICSEARCH:
				break;
			default:
				throw new IllegalArgumentException(String.format(
						"不支持的提供商 type='%s'（可选 in-memory/jdbc/redis/mongodb/elasticsearch）", entity.getType()));
		}
		validateKeyUnique(entity.getKey(), entity.getId());
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	@Override
	public Langchain4jChatMemoryStoreInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, Langchain4jChatMemoryStoreInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	@Override
	public Langchain4jChatMemoryStoreInfo readResource(Resource resource) {
		return read(resource.read());
	}

	private void validateRequired(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + "不能为空");
		}
	}

	private void validateKeyUnique(String key, String currentId) {
		if (key == null || magicResourceService == null) {
			return;
		}
		boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
				.map(it -> (Langchain4jChatMemoryStoreInfo) it)
				.filter(it -> !Objects.equals(it.getId(), currentId))
				.noneMatch(it -> Objects.equals(it.getKey(), key));
		if (!noneMatchKey) {
			throw new IllegalArgumentException(String.format("key '%s' 已存在，请使用其他 key", key));
		}
	}
}
