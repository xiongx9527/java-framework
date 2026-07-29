
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MagicDynamicMqttClientDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientSubscribeScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
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
        } catch (Exception e) {
            log.warn("处理 MQTT Client 订阅文件事件失败", e);
        }
    }

    @EventListener(condition = "#event.type == 'mqttclient-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 MQTT Client 订阅分组事件失败", e);
        }
    }

    private void unregisterByEntity(MqttClientSubscribeInfo entity) {
        this.magicDynamicMqttClientDataSource.unsubscribe(entity);
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
        try {
            boolean result = super.register(entity);
            if (result) {
                registerByEntity(entity);
            }
            return result;
        } catch (Exception e) {
            log.error("MQTT Client 订阅:[{}]注册失败", entity.getTopic(), e);
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<MqttClientSubscribeInfo> mappingNode) {
        try {
            return super.register(mappingNode);
        } catch (Exception e) {
            log.error("MQTT Client 订阅:[{}]映射注册失败", mappingNode.getEntity().getTopic(), e);
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<MqttClientSubscribeInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        try {
            super.unregister(mappingNode);
        } catch (Exception e) {
            log.warn("MQTT Client 订阅:[{}]映射取消注册失败", mappingNode.getEntity().getTopic(), e);
        }
    }
}
