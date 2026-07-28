package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.VectorDataSourceInfo;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;

public class VectorDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<VectorDataSourceInfo> {

    private final MagicDynamicVectorDataSource dataSource;

    public VectorDataSourceMagicDynamicRegistry(MagicResourceStorage<VectorDataSourceInfo> magicResourceStorage,
                                                MagicDynamicVectorDataSource dataSource) {
        super(magicResourceStorage);
        this.dataSource = dataSource;
    }

    @EventListener(condition = "#event.type == 'vector-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @EventListener(condition = "#event.type == 'vector-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean register(VectorDataSourceInfo entity) {
        dataSource.destroyClientByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<VectorDataSourceInfo> mappingNode) {
        VectorDataSourceInfo entity = mappingNode.getEntity();
        dataSource.destroyClientByKey(entity.getKey());
        dataSource.createClient(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<VectorDataSourceInfo> mappingNode) {
        dataSource.destroyClientByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<VectorDataSourceInfo> defaultMappings() {
        return dataSource.datasourceNodes();
    }
}
