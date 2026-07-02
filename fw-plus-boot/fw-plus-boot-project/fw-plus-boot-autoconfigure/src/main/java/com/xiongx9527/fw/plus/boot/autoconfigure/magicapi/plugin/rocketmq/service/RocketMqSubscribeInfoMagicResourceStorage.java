package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service;

import com.alibaba.fastjson.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service.RabbitMqDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.config.KafkaCodeConstants.KAFKA_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT;
import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.config.RocketmqJsonCodeConstants.*;

/**
 * @author zzy
 */
public class RocketMqSubscribeInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<RocketMqSubscribeInfo> {


    @Override
    public boolean requirePath() {
        return false;
    }

    @Override
    public String folder() {
        return "rocketmq-subscribe";
    }

    @Override
    public Class<RocketMqSubscribeInfo> magicClass() {
        return RocketMqSubscribeInfo.class;
    }

    @Override
    public void validate(RocketMqSubscribeInfo entity) {
        notBlank(entity.getKey(), ROCKETMQ_SUBSCRIBE_KEY_REQUIRED);
        notBlank(entity.getTopic(), ROCKETMQ_SUBSCRIBE_TOPIC_REQUIRED);
        List<MagicEntity> files = magicResourceService.files(folder());
        boolean exists = files.stream().filter(x -> !x.getId().equals(entity.getId())).anyMatch(x -> {
            RocketMqSubscribeInfo info = (RocketMqSubscribeInfo) x;
            String key = info.getKey();
            String topic = info.getTopic();
            return key.equals(entity.getKey()) && topic.equals(entity.getTopic());
        });
        isTrue(!exists, ROCKETMQ_SUBSCRIBE_KEY_TOPIC_REPEAT);
        // 如果其他字段不为空，则验证其格式
        if(StringUtils.isNotBlank(entity.getOther())) {
            // 创建JSON验证器，用于验证其他字段的格式
            JSONValidator validator = JSONValidator.from(entity.getOther());
            // 验证其他字段必须是有效的JSON对象
            isTrue(validator.validate() && validator.getType() == JSONValidator.Type.Object,
                    KAFKA_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT);
        }
        RocketMqDataSourceInfoMagicResourceStorage rocketMqDataSourceInfoMagicResourceStorage = SpringUtils.getBean(RocketMqDataSourceInfoMagicResourceStorage.class);
        Map<String, RocketMqDataSourceInfo> rabbitMqDataSourceInfoMap = ListUtils.emptyIfNull(rocketMqDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(RocketMqDataSourceInfo::getKey, val->val,(k1, k2)->k1));
        if (!rabbitMqDataSourceInfoMap.containsKey(entity.getKey())) {
            throw new IllegalArgumentException(String.format("数据源key:%s 不存在", entity.getKey()));
        }
    }

    @Override
    public String buildMappingKey(RocketMqSubscribeInfo info) {
        return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
    }
}
