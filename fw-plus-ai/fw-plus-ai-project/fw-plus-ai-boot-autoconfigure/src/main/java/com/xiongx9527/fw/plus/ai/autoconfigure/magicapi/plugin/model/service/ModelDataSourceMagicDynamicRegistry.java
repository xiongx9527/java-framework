package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;

@Slf4j
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
        } catch (Exception e) {
            log.warn("处理模型数据源文件事件失败，运行态模型可能未更新，异常类型: {}", e.getClass().getSimpleName());
        }
    }

    @EventListener(condition = "#event.type == 'model-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理模型数据源分组事件失败，运行态模型可能未更新，异常类型: {}", e.getClass().getSimpleName());
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
            if (registerMapping(mappingNode)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("注册模型数据源映射失败，已回滚运行态模型，key: {}，异常类型: {}",
                    entity.getKey(), e.getClass().getSimpleName());
        }
        dataSource.destroyClientByKey(entity.getKey());
        return false;
    }

    protected boolean registerMapping(MappingNode<ModelDataSourceInfo> mappingNode) {
        return super.register(mappingNode);
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
