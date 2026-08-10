package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.model.MybatisPlusXmlInfo;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.event.MagicEvent;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class MybatisPlusXmlMagicDynamicRegistry implements MagicDynamicRegistry<MybatisPlusXmlInfo> {

	private final MybatisPlusXmlMagicResourceStorage storage;
	private final MybatisPlusXmlMapperRegistry registry;
	private final ReentrantLock snapshotLock = new ReentrantLock();
	private final Map<String, MybatisPlusXmlInfo> sources = new LinkedHashMap<>();

	public MybatisPlusXmlMagicDynamicRegistry(MybatisPlusXmlMagicResourceStorage storage,
											  MybatisPlusXmlMapperRegistry registry) {
		this.storage = storage;
		this.registry = registry;
	}

	@Override
	public boolean register(MybatisPlusXmlInfo entity) {
		snapshotLock.lock();
		try {
			sources.put(resourceKey(entity), entity.copy());
			registry.rebuild(enabledSources());
			return true;
		} finally {
			snapshotLock.unlock();
		}
	}

	@Override
	public boolean unregister(MybatisPlusXmlInfo entity) {
		snapshotLock.lock();
		try {
			sources.remove(resourceKey(entity));
			registry.rebuild(enabledSources());
			return true;
		} finally {
			snapshotLock.unlock();
		}
	}

	@Override
	public MybatisPlusXmlInfo getMapping(String mappingKey) {
		return null;
	}

	@Override
	public MagicResourceStorage<MybatisPlusXmlInfo> getMagicResourceStorage() {
		return storage;
	}

	@Override
	public List<MybatisPlusXmlInfo> defaultMappings() {
		return Collections.emptyList();
	}

	@EventListener(condition = "#event.type == 'mybatisplus-xml'")
	public void onFileEvent(FileEvent event) {
		MybatisPlusXmlInfo entity = (MybatisPlusXmlInfo) event.getEntity();
		if (event.getAction() == EventAction.DELETE) {
			unregister(entity);
		} else {
			register(entity);
		}
	}

	@EventListener(condition = "#event.type == 'mybatisplus-xml'")
	public void onGroupEvent(GroupEvent event) {
		snapshotLock.lock();
		try {
			for (MagicEntity item : event.getEntities()) {
				MybatisPlusXmlInfo entity = (MybatisPlusXmlInfo) item;
				if (event.getAction() == EventAction.DELETE) {
					sources.remove(resourceKey(entity));
				} else {
					sources.put(resourceKey(entity), entity.copy());
				}
			}
			registry.rebuild(enabledSources());
		} finally {
			snapshotLock.unlock();
		}
	}

	@EventListener(condition = "#event.type == 'clear' && "
			+ "#event.action == T(org.ssssssss.magicapi.core.event.EventAction).CLEAR")
	public void onClear(MagicEvent event) {
		snapshotLock.lock();
		try {
			sources.clear();
			registry.rebuild(enabledSources());
		} finally {
			snapshotLock.unlock();
		}
	}

	private String resourceKey(MybatisPlusXmlInfo entity) {
		String id = entity.getId();
		return id == null || id.trim().isEmpty() ? storage.buildMappingKey(entity) : id;
	}

	private List<MybatisPlusXmlInfo> enabledSources() {
		List<MybatisPlusXmlInfo> enabled = new ArrayList<>();
		for (MybatisPlusXmlInfo source : sources.values()) {
			if (source.isEnabled()) {
				enabled.add(source.copy());
			}
		}
		return enabled;
	}
}
