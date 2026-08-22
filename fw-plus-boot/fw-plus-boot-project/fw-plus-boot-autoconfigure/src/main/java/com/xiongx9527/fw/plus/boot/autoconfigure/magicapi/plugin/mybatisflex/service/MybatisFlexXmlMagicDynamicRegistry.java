package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.model.MybatisFlexXmlInfo;
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

public class MybatisFlexXmlMagicDynamicRegistry implements MagicDynamicRegistry<MybatisFlexXmlInfo> {

	private final MybatisFlexXmlMagicResourceStorage storage;
	private final MybatisFlexXmlMapperRegistry registry;
	private final ReentrantLock snapshotLock = new ReentrantLock();
	private final Map<String, MybatisFlexXmlInfo> sources = new LinkedHashMap<>();

	public MybatisFlexXmlMagicDynamicRegistry(MybatisFlexXmlMagicResourceStorage storage,
											  MybatisFlexXmlMapperRegistry registry) {
		this.storage = storage;
		this.registry = registry;
	}

	@Override
	public boolean register(MybatisFlexXmlInfo entity) {
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
	public boolean unregister(MybatisFlexXmlInfo entity) {
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
	public MybatisFlexXmlInfo getMapping(String mappingKey) {
		return null;
	}

	@Override
	public MagicResourceStorage<MybatisFlexXmlInfo> getMagicResourceStorage() {
		return storage;
	}

	@Override
	public List<MybatisFlexXmlInfo> defaultMappings() {
		return Collections.emptyList();
	}

	@EventListener(condition = "#event.type == 'mybatisflex-xml'")
	public void onFileEvent(FileEvent event) {
		MybatisFlexXmlInfo entity = (MybatisFlexXmlInfo) event.getEntity();
		if (event.getAction() == EventAction.DELETE) {
			unregister(entity);
		} else {
			register(entity);
		}
	}

	@EventListener(condition = "#event.type == 'mybatisflex-xml'")
	public void onGroupEvent(GroupEvent event) {
		snapshotLock.lock();
		try {
			for (MagicEntity item : event.getEntities()) {
				MybatisFlexXmlInfo entity = (MybatisFlexXmlInfo) item;
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

	private String resourceKey(MybatisFlexXmlInfo entity) {
		String id = entity.getId();
		return id == null || id.trim().isEmpty() ? storage.buildMappingKey(entity) : id;
	}

	private List<MybatisFlexXmlInfo> enabledSources() {
		List<MybatisFlexXmlInfo> enabled = new ArrayList<>();
		for (MybatisFlexXmlInfo source : sources.values()) {
			if (source.isEnabled()) {
				enabled.add(source.copy());
			}
		}
		return enabled;
	}
}
