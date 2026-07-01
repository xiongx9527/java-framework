
package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.MqttClientConsumer;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MagicDynamicMqttClientDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientSubscribeInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientSubscribeScriptInfo;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class MqttClientSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MqttClientSubscribeInfo> {

    private final MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource;

    public MqttClientSubscribeMagicDynamicRegistry(MagicResourceStorage<MqttClientSubscribeInfo> magicResourceStorage,
                                                    MagicDynamicMqttClientDataSource magicDynamicMqttClientDataSource) {
        super(magicResourceStorage);
        this.magicDynamicMqttClientDataSource = magicDynamicMqttClientDataSource;
    }

    @EventListener(condition = "#event.type == 'mqttclient-subscribe'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @EventListener(condition = "#event.type == 'mqttclient-subscribe'")
    public void onGroupEvent(GroupEvent event) {
            try {
                processEvent(event);
            } catch (Exception ignored) {
            }
    }

    private void unregisterByEntity(MqttClientSubscribeInfo entity) {
        MqttClientConsumer consumer = this.magicDynamicMqttClientDataSource.getConsumer(entity.getKey());
        if (Objects.nonNull(consumer)) {
            consumer.unsubscribe(entity);
        }
    }

    /**
     * 检查MQTT Client订阅是否启用
     * <p>
     * 该方法首先从实体对象中获取启用状态，然后尝试从配置中心获取对应的配置值进行覆盖。
     * 如果配置获取失败，则默认返回false。
     * </p>
     *
     * @param entity MQTT Client订阅信息实体，包含订阅的Key、Topic等配置信息
     * @return boolean 订阅是否启用，true表示启用，false表示禁用
     */
    private boolean enable(MqttClientSubscribeInfo entity) {
        Boolean enable = entity.isEnabled();
        String enableStr = "false";
        if (enable != null) {
            enableStr = enable.toString();
        }
        try {
            enableStr = MagicAPIConfigUtils.getMqttClientSubscribeConfig(entity.getKey(), entity.getTopic(), "enabled", enableStr);
            enable = Boolean.parseBoolean(enableStr);
        } catch (Exception e) {
            enable = Boolean.FALSE;
        }
        return enable;
    }

    private void registerByEntity(MqttClientSubscribeInfo entity) {
        MagicResourceService resourceService = MagicConfiguration.getMagicResourceService();
        String scriptName = resourceService.getScriptName(entity);
        unregisterByEntity(entity);
        Boolean enable = enable(entity);
        if (Boolean.TRUE.equals(enable)) {
            try {
                log.debug("MQTT Client 订阅:[{}]开始执行", scriptName);
                MqttClientDataSourceInfoMagicResourceStorage dataSourceStorage = SpringUtils.getBean(MqttClientDataSourceInfoMagicResourceStorage.class);
                Map<String, MqttClientDataSourceInfo> dataSourceMap = ListUtils.emptyIfNull(dataSourceStorage.findAll()).stream().collect(Collectors.toMap(MqttClientDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
                if (!dataSourceMap.containsKey(entity.getKey())) {
                    Thread.sleep(5000);
                    dataSourceMap = ListUtils.emptyIfNull(dataSourceStorage.findAll()).stream().collect(Collectors.toMap(MqttClientDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
                    if (!dataSourceMap.containsKey(entity.getKey())) {
                        log.warn("MQTT Client 订阅:[{}]执行出错, 未找到数据源[{}]", scriptName, entity.getKey());
                        return;
                    }
                }
                List<MqttClientSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                newSubscribeList.add(new MqttClientSubscribeScriptInfo(entity, scriptName));
                magicDynamicMqttClientDataSource.putConsumer(dataSourceMap.get(entity.getKey()), newSubscribeList);
                log.debug("MQTT Client 订阅:[{}]执行完毕", scriptName);
            } catch (Exception e) {
                log.error("MQTT Client 订阅:[{}]执行出错, {}", scriptName, e);
            }
        }
    }

    @Override
    public boolean register(MqttClientSubscribeInfo entity) {
        registerByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<MqttClientSubscribeInfo> mappingNode) {
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<MqttClientSubscribeInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }
}