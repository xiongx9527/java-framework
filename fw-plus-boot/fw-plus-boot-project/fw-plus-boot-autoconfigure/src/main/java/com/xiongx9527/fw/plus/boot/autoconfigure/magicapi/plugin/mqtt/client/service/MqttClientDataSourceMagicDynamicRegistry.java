
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MagicDynamicMqttClientDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientSubscribeScriptInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MqttClientDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MqttClientDataSourceInfo> {

    private final MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource;
    private final MqttClientSubscribeInfoMagicResourceStorage subscribeStorage;

    public MqttClientDataSourceMagicDynamicRegistry(MagicResourceStorage<MqttClientDataSourceInfo> magicResourceStorage,
                                                     MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource) {
        this(magicResourceStorage, magicDynamicMqttClientDataSource, null);
    }

    public MqttClientDataSourceMagicDynamicRegistry(MagicResourceStorage<MqttClientDataSourceInfo> magicResourceStorage,
                                                     MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource,
                                                     MqttClientSubscribeInfoMagicResourceStorage subscribeStorage) {
        super(magicResourceStorage);
        this.magicDynamicMqttClientDataSource = magicDynamicMqttClientDataSource;
        this.subscribeStorage = subscribeStorage;
    }

    @EventListener(condition = "#event.type == 'mqttclient-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 MQTT Client 数据源文件事件失败, event={}", event, e);
        }
    }

    @EventListener(condition = "#event.type == 'mqttclient-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 MQTT Client 数据源分组事件失败, event={}", event, e);
        }
    }

    void unregisterByEntity(MqttClientDataSourceInfo info) {
        unregisterProducerByEntity(info);
        this.magicDynamicMqttClientDataSource.deleteConsumer(info.getKey());
    }

    private void unregisterProducerByEntity(MqttClientDataSourceInfo info) {
        this.magicDynamicMqttClientDataSource.deleteProducer(info.getKey());
    }

    private boolean registerByEntity(MqttClientDataSourceInfo info) {
        unregisterProducerByEntity(info);
        log.info("注册 MQTT Client 数据源:[{}]开始执行", info.getKey());
        boolean registerProducer = this.magicDynamicMqttClientDataSource.putProducer(info);
        if (!registerProducer) {
            log.error("MQTT Client 数据源:[{}]注册生产者失败", info.getKey());
            return false;
        }
        log.info("MQTT Client 数据源:[{}]注册生产者成功", info.getKey());
        rebuildConsumersAfterProducerRefresh(info);
        return true;
    }

    private void rebuildConsumersAfterProducerRefresh(MqttClientDataSourceInfo info) {
        if (this.subscribeStorage == null) {
            log.debug("MQTT Client 数据源:[{}]未注入订阅存储，跳过刷新后的消费者重建", info.getKey());
            return;
        }
        try {
            boolean consumerRegistered = this.magicDynamicMqttClientDataSource.replaceConsumerFromSnapshot(info,
                    () -> ListUtils.emptyIfNull(this.subscribeStorage.findAll()).stream()
                            .filter(subscribe -> StringUtils.equals(info.getKey(), subscribe.getKey()))
                            .map(subscribe -> new MqttClientSubscribeScriptInfo(subscribe, subscribe.getTopic()))
                            .collect(Collectors.toList()));
            if (!consumerRegistered) {
                log.error("MQTT Client 数据源:[{}]刷新后重建消费者失败", info.getKey());
            }
        } catch (Exception e) {
            log.warn("MQTT Client 数据源:[{}]刷新后加载订阅并重建消费者失败", info.getKey(), e);
        }
    }

    @Override
    public boolean register(MqttClientDataSourceInfo entity) {
        unregisterProducerByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception e) {
            log.error("MQTT Client 数据源:[{}]注册失败", entity.getKey(), e);
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<MqttClientDataSourceInfo> mappingNode) {
        MqttClientDataSourceInfo entity = mappingNode.getEntity();
        log.info("注册 MQTT Client 数据源:[{}]开始执行", entity.getKey());
        registerByEntity(entity);
        log.info("注册 MQTT Client 数据源：[{}]完成执行", entity.getKey());
        try {
            return super.register(mappingNode);
        } catch (Exception e) {
            log.error("MQTT Client 数据源:[{}]映射注册失败", entity.getKey(), e);
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<MqttClientDataSourceInfo> mappingNode) {
        MqttClientDataSourceInfo entity = mappingNode.getEntity();
        log.info("取消注册 MQTT Client 数据源:[{}]开始执行", entity.getKey());
        unregisterByEntity(entity);
        log.info("取消注册 MQTT Client 数据源：[{}]完成执行", entity.getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception e) {
            log.warn("MQTT Client 数据源:[{}]映射取消注册失败", entity.getKey(), e);
        }
    }

    @Override
    public List<MqttClientDataSourceInfo> defaultMappings() {
        return magicDynamicMqttClientDataSource.producerNodes().stream().filter(it -> it.getId() == null).map(it -> {
            MqttClientDataSourceInfo dataSourceInfo = new MqttClientDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }
}
