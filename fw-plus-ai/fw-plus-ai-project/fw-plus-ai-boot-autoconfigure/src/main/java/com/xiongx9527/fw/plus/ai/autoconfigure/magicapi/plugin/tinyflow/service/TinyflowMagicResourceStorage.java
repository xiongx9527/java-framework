package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.nio.charset.StandardCharsets;

public final class TinyflowMagicResourceStorage implements MagicResourceStorage<TinyflowResource> {

	public static final String FOLDER = "tinyflow";

	private final TinyflowWorkflowValidator validator;

	public TinyflowMagicResourceStorage(TinyflowWorkflowValidator validator) {
		if (validator == null) {
			throw new IllegalArgumentException("validator must not be null");
		}
		this.validator = validator;
	}

	@Override
	public String folder() {
		return FOLDER;
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<TinyflowResource> magicClass() {
		return TinyflowResource.class;
	}

	@Override
	public boolean requirePath() {
		return true;
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
	public TinyflowResource read(byte[] bytes) {
		TinyflowResource resource = new TinyflowResource();
		resource.setContent(new String(bytes, StandardCharsets.UTF_8));
		return resource;
	}

	@Override
	public byte[] write(MagicEntity entity) {
		if (!(entity instanceof TinyflowResource)) {
			throw new IllegalArgumentException("Tinyflow resource type is invalid");
		}
		String content = ((TinyflowResource) entity).getContent();
		if (content == null) {
			throw new IllegalArgumentException("Tinyflow workflow content must not be null");
		}
		return content.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public TinyflowResource readResource(Resource resource) {
		return read(resource.read());
	}

	@Override
	public String buildMappingKey(TinyflowResource resource) {
		return resource == null ? null : resource.getPath();
	}

	@Override
	public void validate(TinyflowResource resource) {
		if (resource == null) {
			throw new IllegalArgumentException("Tinyflow resource must not be null");
		}
		if (isBlank(resource.getId())) {
			throw new IllegalArgumentException("Tinyflow resource id must not be blank");
		}
		if (isBlank(resource.getPath())) {
			throw new IllegalArgumentException("Tinyflow resource path must not be blank");
		}
		validator.validate(resource.getId(), resource.getContent());
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		// Tinyflow resources have no storage-specific dependency on the service.
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
