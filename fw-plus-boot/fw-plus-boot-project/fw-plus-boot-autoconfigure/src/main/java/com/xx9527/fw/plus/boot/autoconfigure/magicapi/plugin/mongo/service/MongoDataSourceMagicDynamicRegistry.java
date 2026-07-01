package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MongoDataSourceInfo;
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
public class MongoDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MongoDataSourceInfo> {

    /**
     * Mongo数据源动态注册器
     * 用于动态注册和管理Debezium数据源的监听引擎
     */
    private final MagicDynamicMongoDataSource magicDynamicMongoDataSource;

    /**
     * 构造函数
     * 初始化Mongo数据源动态注册器
     *
     * @param magicResourceStorage           MagicAPI资源存储器
     * @param magicDynamicDebeziumDataSource Mongo数据源管理器
     */
    public MongoDataSourceMagicDynamicRegistry(MagicResourceStorage<MongoDataSourceInfo> magicResourceStorage, MagicDynamicMongoDataSource magicDynamicDebeziumDataSource) {
        super(magicResourceStorage);
        this.magicDynamicMongoDataSource = magicDynamicDebeziumDataSource;
    }

    /**
     * 处理文件事件
     * 当接收到类型为"mongo-datasource"的文件事件时，调用processEvent方法进行处理
     *
     * @param event 文件事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'mongo-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理分组事件
     * 当接收到类型为"mongo-datasource"的分组事件时，调用processEvent方法进行处理
     *
     * @param event 分组事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'mongo-datasource'")
    public void onGroupEvent(GroupEvent event) {
            try {
                processEvent(event);
            } catch (Exception ignored) {
            }
    }

    /**
     * 注册或注销Debezium数据源
     * 根据数据源的启用状态，启动或停止对应的Debezium监听引擎
     *
     * @param entity Debezium数据源信息对象
     * @return 注册是否成功
     */
    @Override
    public boolean register(MongoDataSourceInfo entity) {
        magicDynamicMongoDataSource.destroyMongoConnectionFactoryByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册映射节点
     * 调用父类方法注册Debezium数据源的映射节点
     *
     * @param mappingNode Debezium数据源映射节点
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<MongoDataSourceInfo> mappingNode) {
        MongoDataSourceInfo entity = mappingNode.getEntity();
        magicDynamicMongoDataSource.destroyMongoConnectionFactoryByKey(entity.getKey());
        magicDynamicMongoDataSource.buildMongoDatabaseFactory(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注销映射节点
     * 停止并销毁对应的数据源监听引擎，完成注销操作
     *
     * @param mappingNode Debezium数据源映射节点
     */
    @Override
    protected void unregister(MappingNode<MongoDataSourceInfo> mappingNode) {
        magicDynamicMongoDataSource.destroyMongoConnectionFactoryByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取默认的映射列表
     * 返回空的Debezium数据源信息列表作为默认映射
     *
     * @return 空的Debezium数据源信息列表
     */
    @Override
    public List<MongoDataSourceInfo> defaultMappings() {
        return magicDynamicMongoDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            MongoDataSourceInfo dataSourceInfo = new MongoDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }
}
