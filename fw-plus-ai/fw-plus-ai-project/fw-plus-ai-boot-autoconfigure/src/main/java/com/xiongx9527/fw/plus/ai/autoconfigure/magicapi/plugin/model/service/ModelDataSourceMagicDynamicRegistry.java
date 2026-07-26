package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;

public class ModelDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<ModelDataSourceInfo> {

    private final MagicDynamicModelDataSource dataSource;

    public ModelDataSourceMagicDynamicRegistry(MagicResourceStorage<ModelDataSourceInfo> magicResourceStorage,
                                                MagicDynamicModelDataSource dataSource) {
        super(magicResourceStorage);
        this.dataSource = dataSource;
    }

    @EventListener(condition = "#event.type == 'model-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @EventListener(condition = "#event.type == 'model-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean register(ModelDataSourceInfo entity) {
        dataSource.destroyClientByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<ModelDataSourceInfo> mappingNode) {
        ModelDataSourceInfo entity = mappingNode.getEntity();
        dataSource.destroyClientByKey(entity.getKey());
        dataSource.createClient(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<ModelDataSourceInfo> mappingNode) {
        dataSource.destroyClientByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<ModelDataSourceInfo> defaultMappings() {
        return dataSource.datasourceNodes();
    }
}
