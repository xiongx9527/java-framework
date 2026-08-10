package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.model.TinyflowResource;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.runtime.TinyflowService;
import com.xiongx9527.fw.plus.ai.tinyflow.workflow.TinyflowWorkflow;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.Collections;
import java.util.List;

public final class TinyflowResourceDynamicRegistry extends AbstractMagicDynamicRegistry<TinyflowResource> {

	private final TinyflowService tinyflowService;

	public TinyflowResourceDynamicRegistry(MagicResourceStorage<TinyflowResource> magicResourceStorage,
			TinyflowService tinyflowService) {
		super(magicResourceStorage);
		if (tinyflowService == null) {
			throw new IllegalArgumentException("tinyflowService must not be null");
		}
		this.tinyflowService = tinyflowService;
	}

	@EventListener(condition = "#event.type == 'tinyflow'")
	public void onFileEvent(FileEvent event) {
		TinyflowResource resource = resource(event == null ? null : event.getEntity());
		if (event != null && event.getAction() == EventAction.DELETE) {
			onDeleted(resource);
		} else {
			onSaved(resource);
		}
	}

	@EventListener(condition = "#event.type == 'tinyflow'")
	public void onGroupEvent(GroupEvent event) {
		if (event == null) {
			return;
		}
		List<org.ssssssss.magicapi.core.model.MagicEntity> entities = event.getEntities();
		for (org.ssssssss.magicapi.core.model.MagicEntity entity
				: entities == null ? Collections.<org.ssssssss.magicapi.core.model.MagicEntity>emptyList() : entities) {
			if (event.getAction() == EventAction.DELETE) {
				onDeleted(resource(entity));
			} else {
				onSaved(resource(entity));
			}
		}
	}

	public void onSaved(TinyflowResource resource) {
		if (!register(resource)) {
			throw new TinyflowException("Tinyflow workflow registration failed: " + resourceId(resource));
		}
	}

	public void onDeleted(TinyflowResource resource) {
		unregister(resource);
	}

	@Override
	public synchronized boolean register(TinyflowResource resource) {
		if (resource == null || isBlank(resource.getId())) {
			throw new TinyflowException("Tinyflow workflow id must not be blank");
		}
		getMagicResourceStorage().validate(resource);
		TinyflowWorkflow workflow = workflow(resource);
		tinyflowService.validate(workflow);
		TinyflowResource existing = getMapping(resource.getPath());
		if (existing != null && !resource.getId().equals(existing.getId())) {
			throw new TinyflowException("Workflow path is ambiguous: " + resource.getPath());
		}
		if (existing != null) {
			tinyflowService.register(workflow);
		}
		return super.register(resource);
	}

	@Override
	protected boolean register(MappingNode<TinyflowResource> mappingNode) {
		tinyflowService.register(workflow(mappingNode.getEntity()));
		return true;
	}

	@Override
	protected void unregister(MappingNode<TinyflowResource> mappingNode) {
		tinyflowService.remove(mappingNode.getEntity().getId());
	}

	private TinyflowResource resource(org.ssssssss.magicapi.core.model.MagicEntity entity) {
		if (!(entity instanceof TinyflowResource)) {
			throw new TinyflowException("Tinyflow resource event contains an invalid entity");
		}
		return (TinyflowResource) entity;
	}

	private TinyflowWorkflow workflow(TinyflowResource resource) {
		return new TinyflowWorkflow(resource.getId(), resource.getPath(), resource.getName(), resource.getContent());
	}

	private String resourceId(TinyflowResource resource) {
		return resource == null ? "<unknown>" : resource.getId();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
