
package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MagicDynamicMqttClientDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientDataSourceInfo;
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
public class MqttClientDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MqttClientDataSourceInfo> {

    private final MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource;

    public MqttClientDataSourceMagicDynamicRegistry(MagicResourceStorage<MqttClientDataSourceInfo> magicResourceStorage,
                                                     MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource) {
        super(magicResourceStorage);
        this.magicDynamicMqttClientDataSource = magicDynamicMqttClientDataSource;
    }

    @EventListener(condition = "#event.type == 'mqttclient-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @EventListener(condition = "#event.type == 'mqttclient-datasource'")
    public void onGroupEvent(GroupEvent event) {
            try {
               processEvent(event);
            } catch (Exception ignored) {
            }
    }

    private void unregisterByEntity(MqttClientDataSourceInfo info) {
        this.magicDynamicMqttClientDataSource.deleteProducer(info.getKey());
    }

    private void registerByEntity(MqttClientDataSourceInfo info) {
        unregisterByEntity(info);
        log.info("注册 MQTT Client 数据源:[{}]开始执行", info.getKey());
        boolean registerProducer = this.magicDynamicMqttClientDataSource.putProducer(info);
        if (!registerProducer) {
            log.error("MQTT Client 数据源:[{}]注册生产者失败", info.getKey());
        } else {
            log.info("MQTT Client 数据源:[{}]注册生产者成功", info.getKey());
        }
    }

    @Override
    public boolean register(MqttClientDataSourceInfo entity) {
        unregisterByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<MqttClientDataSourceInfo> mappingNode) {
        log.info("取消注册 MQTT Client 数据源:[{}]开始执行", mappingNode.getEntity().getKey());
        unregisterByEntity(mappingNode.getEntity());
        log.info("取消注册 MQTT Client 数据源：[{}]完成执行", mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
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