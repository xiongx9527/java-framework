package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.event.MagicEvent;
import org.ssssssss.magicapi.core.service.MagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class JavaEntityMagicDynamicRegistry implements MagicDynamicRegistry<JavaEntityInfo> {

    private final JavaEntityMagicResourceStorage storage;
    private final DynamicEntityRegistry entityRegistry;
    private final ReentrantLock snapshotLock = new ReentrantLock();
    private final Map<String, JavaEntityInfo> sources = new LinkedHashMap<>();

    public JavaEntityMagicDynamicRegistry(JavaEntityMagicResourceStorage storage,
                                          DynamicEntityRegistry entityRegistry) {
        this.storage = storage;
        this.entityRegistry = entityRegistry;
    }

    @Override
    public boolean register(JavaEntityInfo entity) {
        snapshotLock.lock();
        try {
            sources.put(resourceKey(entity), entity.copy());
            return rebuild();
        } finally {
            snapshotLock.unlock();
        }
    }

    @Override
    public boolean unregister(JavaEntityInfo entity) {
        snapshotLock.lock();
        try {
            JavaEntityInfo removed = sources.remove(resourceKey(entity));
            return removed != null && rebuild();
        } finally {
            snapshotLock.unlock();
        }
    }

    @Override
    public JavaEntityInfo getMapping(String mappingKey) {
        snapshotLock.lock();
        try {
            for (JavaEntityInfo source : sources.values()) {
                try {
                    if (!source.isEnabled()) {
                        continue;
                    }
                    if (storage.buildMappingKey(source).equals(mappingKey)) {
                        return source.copy();
                    }
                } catch (RuntimeException ignored) {
                    // Failed candidates remain in the snapshot so a later edit can complete the generation.
                }
            }
            return null;
        } finally {
            snapshotLock.unlock();
        }
    }

    @Override
    public MagicResourceStorage<JavaEntityInfo> getMagicResourceStorage() {
        return storage;
    }

    @Override
    public List<JavaEntityInfo> defaultMappings() {
        return Collections.emptyList();
    }

    @EventListener(condition = "#event.type == 'java-entity'")
    public void onFileEvent(FileEvent event) {
        if (!JavaEntityMagicResourceStorage.RESOURCE_TYPE.equals(event.getType())) {
            return;
        }
        JavaEntityInfo entity = (JavaEntityInfo) event.getEntity();
        if (event.getAction() == EventAction.DELETE) {
            unregister(entity);
        } else {
            register(entity);
        }
    }

    @EventListener(condition = "#event.type == 'java-entity'")
    public void onGroupEvent(GroupEvent event) {
        if (!JavaEntityMagicResourceStorage.RESOURCE_TYPE.equals(event.getType())) {
            return;
        }
        snapshotLock.lock();
        try {
            for (org.ssssssss.magicapi.core.model.MagicEntity item : event.getEntities()) {
                JavaEntityInfo entity = (JavaEntityInfo) item;
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
        return entityRegistry.rebuild(enabledSources());
    }

    private String resourceKey(JavaEntityInfo entity) {
        String id = entity.getId();
        return id == null || id.trim().isEmpty() ? storage.buildMappingKey(entity) : id;
    }

    private List<JavaEntityInfo> enabledSources() {
        List<JavaEntityInfo> enabled = new ArrayList<>();
        for (JavaEntityInfo source : sources.values()) {
            if (source.isEnabled()) {
                enabled.add(source.copy());
            }
        }
        return enabled;
    }
}
