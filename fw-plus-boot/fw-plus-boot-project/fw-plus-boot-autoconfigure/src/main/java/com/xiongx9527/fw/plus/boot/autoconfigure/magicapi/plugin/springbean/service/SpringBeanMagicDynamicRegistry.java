package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.model.SpringBeanInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime.DynamicSpringBeanRegistry;
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

public class SpringBeanMagicDynamicRegistry implements MagicDynamicRegistry<SpringBeanInfo> {

    private final SpringBeanMagicResourceStorage storage;
    private final DynamicSpringBeanRegistry registry;
    private final ReentrantLock snapshotLock = new ReentrantLock();
    private final Map<String, SpringBeanInfo> sources = new LinkedHashMap<>();

    public SpringBeanMagicDynamicRegistry(SpringBeanMagicResourceStorage storage,
                                          DynamicSpringBeanRegistry registry) {
        this.storage = storage;
        this.registry = registry;
    }

    @Override
    public boolean register(SpringBeanInfo entity) {
        snapshotLock.lock();
        try {
            sources.put(resourceKey(entity), entity.copy());
            return rebuild();
        } finally {
            snapshotLock.unlock();
        }
    }

    @Override
    public boolean unregister(SpringBeanInfo entity) {
        snapshotLock.lock();
        try {
            SpringBeanInfo removed = sources.remove(resourceKey(entity));
            return removed != null && rebuild();
        } finally {
            snapshotLock.unlock();
        }
    }

    @Override
    public SpringBeanInfo getMapping(String mappingKey) {
        snapshotLock.lock();
        try {
            for (SpringBeanInfo source : sources.values()) {
                try {
                    if (!source.isEnabled()) {
                        continue;
                    }
                    if (storage.buildMappingKey(source).equals(mappingKey)) {
                        return source.copy();
                    }
                } catch (RuntimeException ignored) {
                }
            }
            return null;
        } finally {
            snapshotLock.unlock();
        }
    }

    @Override
    public MagicResourceStorage<SpringBeanInfo> getMagicResourceStorage() {
        return storage;
    }

    @Override
    public List<SpringBeanInfo> defaultMappings() {
        return Collections.emptyList();
    }

    @EventListener(condition = "#event.type == 'spring-bean'")
    public void onFileEvent(FileEvent event) {
        if (!SpringBeanMagicResourceStorage.RESOURCE_TYPE.equals(event.getType())) {
            return;
        }
        SpringBeanInfo entity = (SpringBeanInfo) event.getEntity();
        if (event.getAction() == EventAction.DELETE) {
            unregister(entity);
        } else {
            register(entity);
        }
    }

    @EventListener(condition = "#event.type == 'spring-bean'")
    public void onGroupEvent(GroupEvent event) {
        if (!SpringBeanMagicResourceStorage.RESOURCE_TYPE.equals(event.getType())) {
            return;
        }
        snapshotLock.lock();
        try {
            for (MagicEntity item : event.getEntities()) {
                SpringBeanInfo entity = (SpringBeanInfo) item;
                if (event.getAction() == EventAction.DELETE) {
                    sources.remove(resourceKey(entity));
                } else {
                    sources.put(resourceKey(entity), entity.copy());
                }
            }
            rebuild();
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
            rebuild();
        } finally {
            snapshotLock.unlock();
        }
    }

    private boolean rebuild() {
        return registry.rebuild(enabledSources());
    }

    private String resourceKey(SpringBeanInfo entity) {
        String id = entity.getId();
        return id == null || id.trim().isEmpty() ? storage.buildMappingKey(entity) : id;
    }

    private List<SpringBeanInfo> enabledSources() {
        List<SpringBeanInfo> enabled = new ArrayList<>();
        for (SpringBeanInfo source : sources.values()) {
            if (source.isEnabled()) {
                enabled.add(source.copy());
            }
        }
        return enabled;
    }
}
