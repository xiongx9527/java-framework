package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model.MqttDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model.MqttSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzy
 */
public class MqttSubscribeInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<MqttSubscribeInfo> implements BaseService {

    /**
     * 获取MQTT订阅配置文件夹名称
     *
     * @return 返回配置文件夹名称"mqtt-subscribe"
     */
    @Override
    public String folder() {
        return "mqtt-subscribe";
    }

    /**
     * 判断是否需要路径信息
     *
     * @return 返回false，表示不需要路径信息
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 获取MQTT订阅信息的实体类类型
     *
     * @return 返回MqttSubscribeInfo.class类对象
     */
    @Override
    public Class<MqttSubscribeInfo> magicClass() {
        return MqttSubscribeInfo.class;
    }

    /**
     * 验证MQTT订阅信息实体的有效性
     *
     * @param entity MQTT订阅信息实体对象
     */
    @Override
    public void validate(MqttSubscribeInfo entity) {
        // 验证key字段为必需字段
        validateRequiredField(entity.getKey(), "key");
        // 验证name字段为必需字段
        validateRequiredField(entity.getName(), "name");
        // 验证businessId字段为必需字段
        validateRequiredField(entity.getTopic(), "Topic");
        // 按key分组收集所有WebSocket订阅信息
        Map<String, List<MqttSubscribeInfo>> map = ListUtils.emptyIfNull(findAll()).stream().collect(Collectors.groupingBy(MqttSubscribeInfo::getKey));
        // 检查同一key下业务标识的唯一性
        if (map.containsKey(entity.getKey())) {
            Map<String, MqttSubscribeInfo> kafkaDataSourceInfoMap = ListUtils.emptyIfNull(map.get(entity.getKey())).stream().collect(Collectors.toMap(MqttSubscribeInfo::getTopic, val -> val, (k1, k2) -> k1));
            if (kafkaDataSourceInfoMap.containsKey(entity.getTopic()) && !kafkaDataSourceInfoMap.get(entity.getTopic()).getId().equals(entity.getId())) {
                throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mqtt.topicExists", entity.getTopic()));
            }
        }
        // 验证数据源key是否存在
        MqttDataSourceInfoMagicResourceStorage kafkaDataSourceInfoMagicResourceStorage = SpringUtils.getBean(MqttDataSourceInfoMagicResourceStorage.class);
        Map<String, MqttDataSourceInfo> kafkaDataSourceInfoMap = ListUtils.emptyIfNull(kafkaDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(MqttDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
        if (!kafkaDataSourceInfoMap.containsKey(entity.getKey())) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.mqtt.datasourceKeyNotExists", entity.getKey()));
        }
    }

    /**
     * 查找所有MQTT订阅信息
     *
     * @return 返回所有MQTT订阅信息的列表
     */
    public List<MqttSubscribeInfo> findAll() {
        return magicResourceService.files(folder());
    }


    /**
     * 构建MQTT订阅信息的映射键值
     *
     * @param info MQTT订阅信息对象
     * @return 返回构建的映射键值字符串
     */
    @Override
    public String buildMappingKey(MqttSubscribeInfo info) {
        return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
    }

}
