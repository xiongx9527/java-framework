package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.resource;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model.Langchain4jSkillInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

/**
 * {@code langchain4j-skill} 资源类型存储：每份 JSON 定义一个 Agent Skill。
 */
public class Langchain4jSkillMagicResourceStorage implements MagicResourceStorage<Langchain4jSkillInfo> {

	private MagicResourceService magicResourceService;

	@Override
	public String folder() {
		return "langchain4j-skill";
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<Langchain4jSkillInfo> magicClass() {
		return Langchain4jSkillInfo.class;
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
	public String buildMappingKey(Langchain4jSkillInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	@Override
	public void validate(Langchain4jSkillInfo entity) {
		validateRequired(entity.getKey(), "key");
		if (!entity.getKey().matches("^[\\w.-]+$")) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.AbstractLangchain4jModelResourceStorage.keyPatternInvalid"));
		}
		validateRequired(entity.getName(), "name");
		validateRequired(entity.getDescription(), "description");
		validateKeyUnique(entity.getKey(), entity.getId());
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	@Override
	public Langchain4jSkillInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, Langchain4jSkillInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	@Override
	public Langchain4jSkillInfo readResource(Resource resource) {
		return read(resource.read());
	}

	private void validateRequired(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.AbstractLangchain4jModelResourceStorage.fieldRequired", field));
		}
	}

	private void validateKeyUnique(String key, String currentId) {
		if (key == null || magicResourceService == null) {
			return;
		}
		boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
				.map(it -> (Langchain4jSkillInfo) it)
				.filter(it -> !Objects.equals(it.getId(), currentId))
				.noneMatch(it -> Objects.equals(it.getKey(), key));
		if (!noneMatchKey) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.AbstractLangchain4jModelResourceStorage.keyDuplicated", key));
		}
	}
}
