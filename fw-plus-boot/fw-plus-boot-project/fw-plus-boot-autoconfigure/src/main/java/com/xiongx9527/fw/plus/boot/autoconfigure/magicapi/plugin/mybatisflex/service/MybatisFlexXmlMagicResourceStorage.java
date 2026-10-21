package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.model.MybatisFlexXmlInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.Objects;

public class MybatisFlexXmlMagicResourceStorage implements MagicResourceStorage<MybatisFlexXmlInfo> {

	public static final String RESOURCE_TYPE = "mybatisflex-xml";

	private final MybatisFlexXmlMapperRegistry registry;
	private MagicResourceService magicResourceService;

	public MybatisFlexXmlMagicResourceStorage(MybatisFlexXmlMapperRegistry registry) {
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
	public Class<MybatisFlexXmlInfo> magicClass() {
		return MybatisFlexXmlInfo.class;
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
	public String buildMappingKey(MybatisFlexXmlInfo entity) {
		return MybatisFlexXmlMapperRegistry.normalizeDbKey(entity.getDbKey()) + ":" + entity.getTableName();
	}

	@Override
	public void validate(MybatisFlexXmlInfo entity) {
		entity.setDbKey(MybatisFlexXmlMapperRegistry.normalizeDbKey(entity.getDbKey()));
		notBlank(entity.getTableName(), I18nMessageResolver.resolveKey("fw.boot.mybatisflex.tableNameRequired"));
		notBlank(entity.getScript(), I18nMessageResolver.resolveKey("fw.boot.mybatisflex.xmlContentRequired"));
		registry.validateMapperXml(entity.getScript());
		validateDbKeyImmutable(entity);
		validateUnique(entity);
		registry.validateDatabaseAndTable(entity.getDbKey(), entity.getTableName());
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	public java.util.List<MybatisFlexXmlInfo> findAll() {
		return magicResourceService.files(folder());
	}

	private void validateDbKeyImmutable(MybatisFlexXmlInfo entity) {
		if (entity.getId() == null || magicResourceService == null) {
			return;
		}
		MybatisFlexXmlInfo old = magicResourceService.file(entity.getId());
		if (old != null && !Objects.equals(MybatisFlexXmlMapperRegistry.normalizeDbKey(old.getDbKey()), entity.getDbKey())) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mybatisflex.dbKeyImmutable"));
		}
	}

	private void validateUnique(MybatisFlexXmlInfo entity) {
		if (magicResourceService == null) {
			return;
		}
		for (MagicEntity item : magicResourceService.files(folder())) {
			MybatisFlexXmlInfo existing = (MybatisFlexXmlInfo) item;
			if (!Objects.equals(existing.getId(), entity.getId())
					&& Objects.equals(MybatisFlexXmlMapperRegistry.normalizeDbKey(existing.getDbKey()), entity.getDbKey())
					&& Objects.equals(existing.getTableName(), entity.getTableName())) {
				throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mybatisflex.dbKeyTableExists"));
			}
		}
	}

	private void notBlank(String value, String message) {
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException(message);
		}
	}
}
