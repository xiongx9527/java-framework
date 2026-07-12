package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MagicDynamicMilvusDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MilvusDataSourceInfo;
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
public class MilvusDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MilvusDataSourceInfo> {

    /** 
     * Milvus数据源管理器实例
     * 用于动态注册和管理Milvus数据源
     */
    private final MagicDynamicMilvusDataSource magicMilvusDataSource;

    /**
     * 构造函数
     * 初始化Milvus数据源动态注册器
     *
     * @param magicResourceStorage MagicAPI资源存储器
     * @param magicDynamicMilvusDataSource Milvus数据源管理器
     */
    public MilvusDataSourceMagicDynamicRegistry(MagicResourceStorage<MilvusDataSourceInfo> magicResourceStorage, MagicDynamicMilvusDataSource magicDynamicMilvusDataSource) {
        super(magicResourceStorage);
        this.magicMilvusDataSource = magicDynamicMilvusDataSource;
    }

    /**
     * 处理Milvus数据源相关的文件事件
     * 当监听到类型为"milvus-datasource"的文件事件时，调用processEvent方法进行处理
     *
     * @param event 文件事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'milvus-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }


    /**
     * 处理Milvus数据源相关的分组事件
     * 当监听到类型为"milvus-datasource"的分组事件时，调用processEvent方法进行处理
     *
     * @param event 分组事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'milvus-datasource'")
    public void onGroupEvent(GroupEvent event) {
            try {
              processEvent(event);
            } catch (Exception ignored) {
            }
    }


    /**
     * 注册Milvus数据源信息
     * 销毁已存在的同名客户端，创建新的客户端，并调用父类的注册方法完成注册
     *
     * @param entity Milvus数据源信息对象
     * @return 注册是否成功
     */
    @Override
    public boolean register(MilvusDataSourceInfo entity) {
        magicMilvusDataSource.destroyClientByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册Milvus数据源映射节点
     * 调用父类的注册方法完成映射节点的注册
     *
     * @param mappingNode Milvus数据源信息的映射节点
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<MilvusDataSourceInfo> mappingNode) {
        MilvusDataSourceInfo entity = mappingNode.getEntity();
        magicMilvusDataSource.destroyClientByKey(entity.getKey());
        magicMilvusDataSource.createClient(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 销毁Milvus数据源映射节点
     * 调用父类的销毁方法完成映射节点的销毁
     *
     * @param mappingNode Milvus数据源信息的映射节点
     */
    @Override
    protected void unregister(MappingNode<MilvusDataSourceInfo> mappingNode) {
        magicMilvusDataSource.destroyClientByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }


    /**
     * 获取默认的Milvus数据源映射列表
     * 目前返回空列表，表示没有默认的数据源映射
     *
     * @return List<MilvusDataSourceInfo> 空的Milvus数据源信息列表
     */
    @Override
    public List<MilvusDataSourceInfo> defaultMappings() {
        return magicMilvusDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            MilvusDataSourceInfo dataSourceInfo = new MilvusDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }
}
