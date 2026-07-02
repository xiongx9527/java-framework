package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.MagicDynamicDebeziumDataSource;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DebeziumDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<DebeziumDataSourceInfo> {

    /**
     * Debezium数据源动态注册器
     * 用于动态注册和管理Debezium数据源的监听引擎
     */
    private final MagicDynamicDebeziumDataSource magicDynamicDebeziumDataSource;

    /**
     * 构造函数
     * 初始化Debezium数据源动态注册器
     *
     * @param magicResourceStorage           MagicAPI资源存储器
     * @param magicDynamicDebeziumDataSource Debezium数据源管理器
     */
    public DebeziumDataSourceMagicDynamicRegistry(MagicResourceStorage<DebeziumDataSourceInfo> magicResourceStorage, MagicDynamicDebeziumDataSource magicDynamicDebeziumDataSource) {
        super(magicResourceStorage);
        this.magicDynamicDebeziumDataSource = magicDynamicDebeziumDataSource;
    }

    /**
     * 处理文件事件
     * 当接收到类型为"debezium-datasource"的文件事件时，调用processEvent方法进行处理
     *
     * @param event 文件事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'debezium-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("处理文件事件时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理分组事件
     * 当接收到类型为"debezium-datasource"的分组事件时，调用processEvent方法进行处理
     *
     * @param event 分组事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'debezium-datasource'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }

    /**
     * 注册或注销Debezium数据源服务
     * 根据数据源的启用状态，启动或停止对应的Debezium监听引擎
     *
     * @param entity Debezium数据源信息对象
     */
    private void registerService(DebeziumDataSourceInfo entity) {
        Boolean enable = entity.isEnabled();
        if (enable != null) {
            String enableStr = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.enabled", entity.getKey()), enable.toString());
            try {
                enable = Boolean.parseBoolean(enableStr);
            } catch (Exception e) {
                enable = Boolean.FALSE;
            }
        }
        if (Boolean.TRUE.equals(enable)) {
            magicDynamicDebeziumDataSource.createEngineAndRun(entity);
            log.info("已注册并启动 Debezium 数据源监听: {}", entity.getKey());
        } else {
            magicDynamicDebeziumDataSource.destroyEngine(entity.getKey());
            log.info("已注销 Debezium 数据源监听: {}", entity.getKey());
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
    public boolean register(DebeziumDataSourceInfo entity) {
        magicDynamicDebeziumDataSource.destroyEngine(entity.getKey());
        return super.register(entity);
    }

    /**
     * 注册映射节点
     * 调用父类方法注册Debezium数据源的映射节点
     *
     * @param mappingNode Debezium数据源映射节点
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<DebeziumDataSourceInfo> mappingNode) {
        registerService(mappingNode.getEntity());
        return super.register(mappingNode);
    }

    /**
     * 注销映射节点
     * 停止并销毁对应的数据源监听引擎，完成注销操作
     *
     * @param mappingNode Debezium数据源映射节点
     */
    @Override
    protected void unregister(MappingNode<DebeziumDataSourceInfo> mappingNode) {
        DebeziumDataSourceInfo entity = mappingNode.getEntity();
        magicDynamicDebeziumDataSource.destroyEngine(entity.getKey());
        log.info("已注销 Debezium 数据源监听: {}", entity.getKey());
        super.unregister(mappingNode);
    }

    /**
     * 获取默认的映射列表
     * 返回空的Debezium数据源信息列表作为默认映射
     *
     * @return 空的Debezium数据源信息列表
     */
    @Override
    public List<DebeziumDataSourceInfo> defaultMappings() {
        return new ArrayList<>();
    }
}
