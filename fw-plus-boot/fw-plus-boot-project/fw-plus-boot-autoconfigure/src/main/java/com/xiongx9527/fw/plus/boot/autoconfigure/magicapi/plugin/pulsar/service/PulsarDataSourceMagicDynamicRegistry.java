package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.PulsarConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.MagicDynamicPulsarDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.Producer;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pulsar 数据源 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class PulsarDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<PulsarDataSourceInfo> {
    /**
     * MagicDynamicPulsarDataSource实例，用于管理Pulsar数据源的动态配置和连接
     *
     * 该字段为final类型，确保在对象初始化后不可更改，保证数据源配置的稳定性。
     * 通过该数据源可以实现对Pulsar集群的动态连接管理和配置更新。
     */
    private final MagicDynamicPulsarDataSource magicDynamicPulsarDataSource;

    /**
     * 构造函数，用于创建Pulsar数据源魔法动态注册器实例
     *
     * @param magicResourceStorage 魔法资源存储器，用于存储Pulsar数据源信息
     * @param magicDynamicPulsarDataSource 魔法动态Pulsar数据源，用于动态管理Pulsar数据源
     */
    public PulsarDataSourceMagicDynamicRegistry(MagicResourceStorage<PulsarDataSourceInfo> magicResourceStorage,
                                                MagicDynamicPulsarDataSource magicDynamicPulsarDataSource) {
        super(magicResourceStorage);
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
    }


    /**
     * 处理Pulsar数据源相关的文件事件
     * 当监听到类型为'pulsar-datasource'的文件事件时，调用processEvent方法进行处理
     *
     * @param event 文件事件对象，包含事件类型和其他相关信息
     */
    @EventListener(condition = "#event.type == 'pulsar-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("处理文件事件时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理Pulsar数据源相关的分组事件
     * 当监听到类型为'pulsar-datasource'的分组事件时，调用processEvent方法进行处理
     *
     * @param event 分组事件对象，包含事件类型和其他相关信息
     */
    @EventListener(condition = "#event.type == 'pulsar-datasource'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }

    /**
     * 根据实体取消注册Pulsar数据源，包括生产者和消费者
     *
     * @param info Pulsar数据源信息实体
     */
    private void unregisterByEntity(PulsarDataSourceInfo info) {
        this.magicDynamicPulsarDataSource.deleteProducer(info.getKey());
    }


    /**
     * 根据实体注册Pulsar数据源，包括生产者和消费者
     *
     * @param info Pulsar数据源信息实体
     */
    private void registerByEntity(PulsarDataSourceInfo info) {
        unregisterByEntity(info);
        log.info("注册 Pulsar 消息队列:[{}]开始执行", info.getKey());
        // 注册生产者
        boolean registerProducer = this.magicDynamicPulsarDataSource.putProducer(info);
        if (!registerProducer) {
            log.error("pulsar 消息队列:[{}]注册生产者失败", info.getKey());
        } else {
            log.info("pulsar 消息队列:[{}]注册生产者成功", info.getKey());
        }
    }

    /**
     * 注册Pulsar数据源
     *
     * @param entity Pulsar数据源信息实体
     * @return 注册是否成功
     */
    @Override
    public boolean register(PulsarDataSourceInfo entity) {
        unregisterByEntity(entity);
        return super.register(entity);
    }

    /**
     * 注册Pulsar数据源映射节点
     *
     * @param mappingNode 包含Pulsar数据源信息的映射节点
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<PulsarDataSourceInfo> mappingNode) {
        PulsarDataSourceInfo entity = mappingNode.getEntity();
        log.info("注册 Pulsar 消息队列:[{}]开始执行", entity.getKey());
        registerByEntity(entity);
        log.info("注册 Pulsar 消息队列：[{}]完成执行", entity.getKey());
        return super.register(mappingNode);
    }

    /**
     * 取消注册Pulsar数据源映射节点
     *
     * @param mappingNode 包含Pulsar数据源信息的映射节点
     */
    @Override
    protected void unregister(MappingNode<PulsarDataSourceInfo> mappingNode) {
        log.info("取消注册 Pulsar 消息队列:[{}]开始执行", mappingNode.getEntity().getKey());
        unregisterByEntity(mappingNode.getEntity());
        log.info("取消注册 Pulsar 消息队列：[{}]完成执行", mappingNode.getEntity().getKey());
        super.unregister(mappingNode);
    }

    /**
     * 获取默认的Pulsar数据源映射列表
     *
     * @return Pulsar数据源信息列表
     */
    @Override
    public List<PulsarDataSourceInfo> defaultMappings() {
        // 从生产者节点中获取没有ID的节点，并转换为数据源信息列表
        List<PulsarDataSourceInfo> dataSourceInfoList = magicDynamicPulsarDataSource.producerNodes().stream().filter(it -> it.getId() == null).map(it -> {
            PulsarDataSourceInfo dataSourceInfo = new PulsarDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
        try {
            Producer producer = SpringUtils.getBean(Producer.class);
            if (producer != null) {
                PulsarDataSourceInfo dataSourceInfo = new PulsarDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

}
