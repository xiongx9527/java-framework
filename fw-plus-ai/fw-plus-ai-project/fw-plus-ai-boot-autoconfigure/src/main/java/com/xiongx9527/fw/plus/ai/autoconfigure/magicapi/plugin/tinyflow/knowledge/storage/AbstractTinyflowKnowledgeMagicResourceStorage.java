package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.TinyflowKnowledgeMagicResource;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

abstract class AbstractTinyflowKnowledgeMagicResourceStorage
		implements MagicResourceStorage<TinyflowKnowledgeMagicResource> {

	@Override public final String suffix() { return ".json"; }
	@Override public final Class<TinyflowKnowledgeMagicResource> magicClass() { return TinyflowKnowledgeMagicResource.class; }
	@Override public final boolean requirePath() { return false; }
	@Override public final boolean requiredScript() { return false; }
	@Override public final boolean allowRoot() { return true; }
	@Override public final String buildMappingKey(TinyflowKnowledgeMagicResource entity) { return entity == null ? null : entity.getId(); }
	@Override public final TinyflowKnowledgeMagicResource read(byte[] bytes) {
		return JsonUtils.readValue(bytes, TinyflowKnowledgeMagicResource.class);
	}
	@Override public final byte[] write(MagicEntity entity) {
		if (!(entity instanceof TinyflowKnowledgeMagicResource)) {
			throw new IllegalArgumentException("Tinyflow knowledge resource type is invalid");
		}
		return JsonUtils.toJsonBytes(entity);
	}
	@Override public final void setMagicResourceService(MagicResourceService magicResourceService) { }

	void common(TinyflowKnowledgeMagicResource entity) {
		if (entity == null) throw new IllegalArgumentException("resource must not be null");
		required(entity.getTenantId(), "tenantId");
	}

	void required(Object value, String field) {
		if (value == null || value instanceof String && ((String) value).trim().length() == 0) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
	}
}
