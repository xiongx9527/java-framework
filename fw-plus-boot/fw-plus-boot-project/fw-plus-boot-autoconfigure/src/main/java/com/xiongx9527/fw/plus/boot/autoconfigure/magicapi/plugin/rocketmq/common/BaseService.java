package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.common;

import com.alibaba.fastjson.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service.RocketMqDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.IoUtils;

import java.util.Objects;

import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.config.KafkaCodeConstants.KAFKA_DATASOURCE_OTHER_MUST_BE_JSONOBJECT;
import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.config.RocketmqJsonCodeConstants.ROCKETMQ_DATASOURCE_NAMESERVER_REQUIRED;

public interface BaseService extends MagicBaseService, JsonCodeConstants {

    /**
     * 验证RocketMQ数据源信息字段的有效性
     *
     * @param entity 待验证的RocketMQ数据源信息实体对象
     */
    default void validateField(RocketMqDataSourceInfo entity) {
        RocketMqDataSourceInfoMagicResourceStorage dd = SpringUtils.getBean(RocketMqDataSourceInfoMagicResourceStorage.class);
        MagicResourceService magicResourceService = SpringUtils.getBean(MagicResourceService.class);

        // 验证必要字段不为空
        notBlank(entity.getKey(), DS_KEY_REQUIRED);
        notBlank(entity.getNameServer(), ROCKETMQ_DATASOURCE_NAMESERVER_REQUIRED);

        // 验证key字段符合文件名规范
        isTrue(IoUtils.validateFileName(entity.getKey()), DATASOURCE_KEY_INVALID);

        // 验证key字段在同级目录下唯一性
        boolean noneMatchKey = magicResourceService.listFiles(dd.folder() + ":0").stream()
                .map(it -> (RocketMqDataSourceInfo)it)
                .filter(it -> !it.getId().equals(entity.getId()))
                .noneMatch(it -> Objects.equals(it.getKey(), entity.getKey()));
        isTrue(noneMatchKey, DS_KEY_CONFLICT);

        // 验证已存在记录的key字段不可修改
        if (entity.getId() != null) {
            RocketMqDataSourceInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
            if (null != oldKafkaDataSourceInfo) {
                if (!oldKafkaDataSourceInfo.getKey().equals(entity.getKey())) {
                    throw new IllegalArgumentException("key不可修改");
                }
            }
        }

        // 验证other字段是否为有效的JSON对象
        if(StringUtils.isNotBlank(entity.getOther())) {
            JSONValidator validator = JSONValidator.from(entity.getOther());
            isTrue(validator.validate() && validator.getType() == JSONValidator.Type.Object,
                    KAFKA_DATASOURCE_OTHER_MUST_BE_JSONOBJECT);
        }
    }
}
