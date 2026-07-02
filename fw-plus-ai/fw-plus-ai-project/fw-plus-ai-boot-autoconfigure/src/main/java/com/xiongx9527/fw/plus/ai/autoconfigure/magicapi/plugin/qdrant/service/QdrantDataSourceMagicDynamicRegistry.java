package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.MagicDynamicQdrantDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.QdrantDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class QdrantDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<QdrantDataSourceInfo> {

        /** 动态Qdrant数据源 */
    private final MagicDynamicQdrantDataSource magicQdrantDataSource;


    /**
     * Qdrant数据源动态注册器构造函数
     *
     * @param magicResourceStorage Qdrant数据源信息存储器，用于管理数据源的存储和检索
     * @param magicDynamicQdrantDataSource 动态Qdrant数据源实例，用于实际的数据源操作
     */
    public QdrantDataSourceMagicDynamicRegistry(MagicResourceStorage<QdrantDataSourceInfo> magicResourceStorage,
                                                MagicDynamicQdrantDataSource magicDynamicQdrantDataSource) {
        super(magicResourceStorage);
        this.magicQdrantDataSource = magicDynamicQdrantDataSource;
    }


    /**
     * 处理文件事件
     * 
     * @param event 文件事件
     */
    @EventListener(condition = "#event.type == 'qdrant-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("处理文件事件时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理分组事件
     * 
     * @param event 分组事件
     */
    @EventListener(condition = "#event.type == 'qdrant-datasource'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }
    
    /**
     * 注册Qdrant数据源信息
     * 
     * @param entity Qdrant数据源信息
     * @return 是否注册成功
     */
    @Override
    public boolean register(QdrantDataSourceInfo entity) {
        magicQdrantDataSource.destroyClientByKey(entity.getKey());
        return super.register(entity);
    }

    /**
     * 注册映射节点
     * 
     * @param mappingNode 映射节点
     * @return 是否注册成功
     */
    @Override
    protected boolean register(MappingNode<QdrantDataSourceInfo> mappingNode) {
        QdrantDataSourceInfo entity = mappingNode.getEntity();
        magicQdrantDataSource.destroyClientByKey(entity.getKey());
        magicQdrantDataSource.createClient(entity);
        return super.register(mappingNode);
    }

    /**
     * 注销映射节点
     * 
     * @param mappingNode 映射节点
     */
    @Override
    protected void unregister(MappingNode<QdrantDataSourceInfo> mappingNode) {
        magicQdrantDataSource.destroyClientByKey(mappingNode.getEntity().getKey());
        super.unregister(mappingNode);
    }

    /**
     * 获取默认映射列表
     * 
     * @return Qdrant数据源信息列表
     */
    @Override
    public List<QdrantDataSourceInfo> defaultMappings() {
        return magicQdrantDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            QdrantDataSourceInfo dataSourceInfo = new QdrantDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }
}
