package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.model.MybatisPlusXmlInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.Objects;

public class MybatisPlusXmlMagicResourceStorage implements MagicResourceStorage<MybatisPlusXmlInfo> {

	public static final String RESOURCE_TYPE = "mybatisplus-xml";

	private final MybatisPlusXmlMapperRegistry registry;
	private MagicResourceService magicResourceService;

	public MybatisPlusXmlMagicResourceStorage(MybatisPlusXmlMapperRegistry registry) {
		this.registry = registry;
	}

	@Override
	public String folder() {
		return RESOURCE_TYPE;
	}

	@Override
	public String suffix() {
		return ".xml";
	}

	@Override
	public Class<MybatisPlusXmlInfo> magicClass() {
		return MybatisPlusXmlInfo.class;
	}

	@Override
	public boolean requirePath() {
		return true;
	}

	@Override
	public boolean requiredScript() {
		return true;
	}

	@Override
	public boolean allowRoot() {
		return true;
	}

	@Override
	public String buildMappingKey(MybatisPlusXmlInfo entity) {
		return MybatisPlusXmlMapperRegistry.normalizeDbKey(entity.getDbKey()) + ":" + entity.getTableName();
	}

	@Override
	public void validate(MybatisPlusXmlInfo entity) {
		entity.setDbKey(MybatisPlusXmlMapperRegistry.normalizeDbKey(entity.getDbKey()));
		notBlank(entity.getTableName(), "表名不能为空");
		notBlank(entity.getScript(), "MyBatis-Plus XML 内容不能为空");
		registry.validateMapperXml(entity.getScript());
		validateDbKeyImmutable(entity);
		validateUnique(entity);
		registry.validateDatabaseAndTable(entity.getDbKey(), entity.getTableName());
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	public java.util.List<MybatisPlusXmlInfo> findAll() {
		return magicResourceService.files(folder());
	}

	private void validateDbKeyImmutable(MybatisPlusXmlInfo entity) {
		if (entity.getId() == null || magicResourceService == null) {
			return;
		}
		MybatisPlusXmlInfo old = magicResourceService.file(entity.getId());
		if (old != null && !Objects.equals(MybatisPlusXmlMapperRegistry.normalizeDbKey(old.getDbKey()), entity.getDbKey())) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mybatisplus.dbKeyImmutable"));
		}
	}

	private void validateUnique(MybatisPlusXmlInfo entity) {
		if (magicResourceService == null) {
			return;
		}
		for (MagicEntity item : magicResourceService.files(folder())) {
			MybatisPlusXmlInfo existing = (MybatisPlusXmlInfo) item;
			if (!Objects.equals(existing.getId(), entity.getId())
					&& Objects.equals(MybatisPlusXmlMapperRegistry.normalizeDbKey(existing.getDbKey()), entity.getDbKey())
					&& Objects.equals(existing.getTableName(), entity.getTableName())) {
				throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mybatisplus.dbKeyTableExists"));
			}
		}
	}

	private void notBlank(String value, String message) {
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException(message);
		}
	}
}
