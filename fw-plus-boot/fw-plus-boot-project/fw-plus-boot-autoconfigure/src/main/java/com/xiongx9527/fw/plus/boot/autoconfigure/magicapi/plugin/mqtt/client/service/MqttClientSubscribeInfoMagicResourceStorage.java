
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.common.MqttClientBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientSubscribeInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MqttClientSubscribeInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<MqttClientSubscribeInfo> implements MqttClientBaseService {

    @Override
    public String folder() {
        return "mqttclient-subscribe";
    }

    @Override
    public boolean requirePath() {
        return false;
    }

    @Override
    public Class<MqttClientSubscribeInfo> magicClass() {
        return MqttClientSubscribeInfo.class;
    }

    @Override
    public void validate(MqttClientSubscribeInfo entity) {
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getTopic(), "Topic");
        Map<String, List<MqttClientSubscribeInfo>> map = ListUtils.emptyIfNull(findAll()).stream().collect(Collectors.groupingBy(MqttClientSubscribeInfo::getKey));
        if (map.containsKey(entity.getKey())) {
            Map<String, MqttClientSubscribeInfo> topicMap = ListUtils.emptyIfNull(map.get(entity.getKey())).stream().collect(Collectors.toMap(MqttClientSubscribeInfo::getTopic, val -> val, (k1, k2) -> k1));
            if (topicMap.containsKey(entity.getTopic()) && !topicMap.get(entity.getTopic()).getId().equals(entity.getId())) {
                throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mqtt.client.topicExists", entity.getTopic()));
            }
        }
        MqttClientDataSourceInfoMagicResourceStorage dataSourceStorage = SpringUtils.getBean(MqttClientDataSourceInfoMagicResourceStorage.class);
        Map<String, MqttClientDataSourceInfo> dataSourceMap = ListUtils.emptyIfNull(dataSourceStorage.findAll()).stream().collect(Collectors.toMap(MqttClientDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
        if (!dataSourceMap.containsKey(entity.getKey())) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mqtt.client.datasourceKeyNotExists", entity.getKey()));
        }
    }

    public List<MqttClientSubscribeInfo> findAll() {
        return magicResourceService.files(folder());
    }

    @Override
    public String buildMappingKey(MqttClientSubscribeInfo info) {
        return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
    }
}