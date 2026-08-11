package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflowValidator;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.nio.charset.StandardCharsets;

public final class TinyflowMagicResourceStorage implements MagicResourceStorage<TinyflowResource> {

	public static final String FOLDER = "tinyflow";

	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String GROUP_ID = "groupId";
	private static final String PATH = "path";
	private static final String CONTENT = "content";
	private static final String SCRIPT = "script";

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
		String json = new String(bytes, StandardCharsets.UTF_8);
		JSONObject object = JSON.parseObject(json);
		if (object == null) {
			throw new IllegalArgumentException("Tinyflow workflow content must be a JSON object");
		}
		if (object.containsKey(CONTENT) || object.containsKey(SCRIPT)) {
			return transportResource(object);
		}
		return physicalResource(object);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		if (!(entity instanceof TinyflowResource)) {
			throw new IllegalArgumentException("Tinyflow resource type is invalid");
		}
		TinyflowResource resource = (TinyflowResource) entity;
		String content = resource.getContent();
		if (content == null) {
			throw new IllegalArgumentException("Tinyflow workflow content must not be null");
		}
		JSONObject workflow = JSON.parseObject(content);
		if (workflow == null) {
			throw new IllegalArgumentException("Tinyflow workflow content must be a JSON object");
		}
		workflow.remove(CONTENT);
		workflow.remove(SCRIPT);
		workflow.put(ID, resource.getId());
		workflow.put(NAME, resource.getName());
		workflow.put(GROUP_ID, resource.getGroupId());
		workflow.put(PATH, resource.getPath());
		return workflow.toJSONString().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public TinyflowResource readResource(Resource resource) {
		TinyflowResource result = read(resource.read());
		String resourceName = stripSuffix(resource.name());
		if (isBlank(result.getName())) {
			result.setName(resourceName);
		}
		if (isBlank(result.getId())) {
			result.setId(resourceName);
		}
		if (isBlank(result.getGroupId())) {
			result.setGroupId(FOLDER + ":0");
		}
		if (isBlank(result.getPath())) {
			result.setPath(resourceName);
		}
		return result;
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

	private TinyflowResource transportResource(JSONObject object) {
		TinyflowResource resource = metadata(object);
		String content = object.getString(CONTENT);
		resource.setContent(content == null ? object.getString(SCRIPT) : content);
		return resource;
	}

	private TinyflowResource physicalResource(JSONObject object) {
		TinyflowResource resource = metadata(object);
		object.remove(ID);
		object.remove(NAME);
		object.remove(GROUP_ID);
		object.remove(PATH);
		resource.setContent(object.toJSONString());
		return resource;
	}

	private TinyflowResource metadata(JSONObject object) {
		TinyflowResource resource = new TinyflowResource();
		resource.setId(object.getString(ID));
		resource.setName(object.getString(NAME));
		resource.setGroupId(object.getString(GROUP_ID));
		resource.setPath(object.getString(PATH));
		return resource;
	}

	private String stripSuffix(String name) {
		return name != null && name.endsWith(suffix()) ? name.substring(0, name.length() - suffix().length()) : name;
	}
}
