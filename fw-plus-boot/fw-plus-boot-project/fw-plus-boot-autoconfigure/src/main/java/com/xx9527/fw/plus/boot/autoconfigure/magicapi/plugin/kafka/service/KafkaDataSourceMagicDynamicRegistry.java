package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.MagicDynamicKafkaDataSource;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Kafka 数据源 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class KafkaDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<KafkaDataSourceInfo> {
    /** Kafka动态数据源，用于管理Kafka连接 */
	private final MagicDynamicKafkaDataSource magicDynamicKafkaDataSource;

    /**
     * 构造函数，初始化Kafka数据源动态注册器
     *
     * @param magicResourceStorage      Kafka数据源信息资源存储器
     * @param magicDynamicKafkaDataSource Kafka动态数据源
     */
    public KafkaDataSourceMagicDynamicRegistry(MagicResourceStorage<KafkaDataSourceInfo> magicResourceStorage,
                                               MagicDynamicKafkaDataSource magicDynamicKafkaDataSource) {
        super(magicResourceStorage);
        this.magicDynamicKafkaDataSource = magicDynamicKafkaDataSource;
    }

    /**
     * 监听并处理Kafka数据源的文件事件
     * 当接收到类型为"kafka-datasource"的文件事件时触发此方法，将事件传递给processEvent方法进行处理
     * 
     * @param event 文件事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'kafka-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听并处理Kafka数据源的分组事件
     * 当接收到类型为"kafka-datasource"的分组事件时触发此方法，将事件传递给processEvent方法进行处理
     * 
     * @param event 分组事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'kafka-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 根据实体取消注册Kafka数据源，包括生产者和消费者
     *
     * @param info Kafka数据源信息实体
     */
    private void unregisterByEntity(KafkaDataSourceInfo info) {
        this.magicDynamicKafkaDataSource.deleteProducer(info.getKey());
    }


    /**
     * 根据实体注册Kafka数据源，包括生产者
     *
     * @param info Kafka数据源信息实体
     */
    private void registerByEntity(KafkaDataSourceInfo info) {
        unregisterByEntity(info);
        log.info("注册 Kafka 消息队列:[{}]开始执行", info.getKey());
        // 注册生产者
        boolean registerProducer = this.magicDynamicKafkaDataSource.putProducer(info);
        if (!registerProducer) {
            log.error("Kafka 消息队列:[{}]注册生产者失败", info.getKey());
        } else {
            log.info("Kafka 消息队列:[{}]注册生产者成功", info.getKey());
        }
    }

    @Override
    public boolean register(KafkaDataSourceInfo entity) {
        unregisterByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<KafkaDataSourceInfo> mappingNode) {
        registerByEntity(mappingNode.getEntity());
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<KafkaDataSourceInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取默认的Kafka数据源映射列表
     *
     * @return Kafka数据源信息列表
     */
    @Override
    public List<KafkaDataSourceInfo> defaultMappings() {
        // 从生产者节点中获取没有ID的节点，并转换为数据源信息列表
        List<KafkaDataSourceInfo> dataSourceInfoList = this.magicDynamicKafkaDataSource.producerNodes().stream().filter(it -> it.getId() == null).map(it -> {
            KafkaDataSourceInfo dataSourceInfo = new KafkaDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
        try {
            KafkaTemplate kafkaTemplate = SpringUtils.getBean(KafkaTemplate. class);
            if (kafkaTemplate != null
                    && kafkaTemplate.getProducerFactory() != null ) {
                Object bootstrapServers = kafkaTemplate.getProducerFactory()
                        .getConfigurationProperties().get("bootstrap.servers");
                if (Objects.nonNull(bootstrapServers) ) {
                    KafkaDataSourceInfo dataSourceInfo = new KafkaDataSourceInfo();
                    dataSourceInfo.setKey("default");
                    dataSourceInfo.setName("默认数据源");
                    dataSourceInfoList.add(dataSourceInfo);
                }
            }
        } catch (Exception e) {
            log.debug("获取默认 KafkaTemplate 失败", e);
        }
        return dataSourceInfoList;
    }

}
