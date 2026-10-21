
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.common.MqttClientBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.List;
import java.util.Objects;

public class MqttClientDataSourceInfoMagicResourceStorage implements MagicResourceStorage<MqttClientDataSourceInfo>, MqttClientBaseService {

    private MagicResourceService magicResourceService;

    @Override
    public String folder() {
        return "mqttclient-datasource";
    }

    @Override
    public String suffix() {
        return ".json";
    }

    @Override
    public Class<MqttClientDataSourceInfo> magicClass() {
        return MqttClientDataSourceInfo.class;
    }

    @Override
    public boolean requirePath() {
        return false;
    }

    @Override
    public boolean requiredScript() {
        return false;
    }

    @Override
    public boolean allowRoot() {
        return true;
    }

    @Override
    public String buildMappingKey(MqttClientDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    @Override
    public void validate(MqttClientDataSourceInfo entity) {
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);
        if (entity.getId() != null) {
            MqttClientDataSourceInfo oldInfo = magicResourceService.file(entity.getId());
            if (null != oldInfo) {
                if (!oldInfo.getKey().equals(entity.getKey())) {
                    throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mqtt.client.keyImmutable"));
                }
            }
        }
    }

    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (MqttClientDataSourceInfo) it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));
        if (!noneMatchKey) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mqtt.client.datasourceKeyExists", key));
        }
    }

    public List<MqttClientDataSourceInfo> findAll() {
        return magicResourceService.files(folder());
    }

    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    @Override
    public MqttClientDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, MqttClientDataSourceInfo.class);
    }

    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    @Override
    public MqttClientDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }
}