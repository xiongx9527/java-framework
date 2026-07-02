package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.common;

import com.alibaba.fastjson.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service.RabbitMqDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.starter.MagicRabbitMqConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.IoUtils;

import java.util.Map;
import java.util.Objects;

import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.config.KafkaCodeConstants.KAFKA_DATASOURCE_OTHER_MUST_BE_JSONOBJECT;
import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.config.RabbitmqJsonCodeConstants.RABBITMQ_DATASOURCE_HOST_REQUIRED;
import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.config.RabbitmqJsonCodeConstants.RABBITMQ_DATASOURCE_PORT_REQUIRED;

public interface BaseService extends MagicBaseService , JsonCodeConstants {


    /**
     * 验证RabbitMQ数据源字段的有效性
     *
     * @param entity 待验证的RabbitMQ数据源信息实体对象，包含数据源的各种配置信息
     */
    default void validateField(RabbitMqDataSourceInfo entity) {
        RabbitMqDataSourceInfoMagicResourceStorage dd = SpringUtils.getBean(RabbitMqDataSourceInfoMagicResourceStorage.class);
        MagicResourceService magicResourceService = SpringUtils.getBean(MagicResourceService.class);

        // 基础字段非空验证
        this.notBlank(entity.getKey(), DS_KEY_REQUIRED);
        this.notBlank(entity.getHost(), RABBITMQ_DATASOURCE_HOST_REQUIRED);
        this.notNull(entity.getPort(), RABBITMQ_DATASOURCE_PORT_REQUIRED);
        this.isTrue(IoUtils.validateFileName(entity.getKey()), DATASOURCE_KEY_INVALID);

        // 检查 key 是否与已有数据源冲突（排除自身）
        boolean noneMatchKey = magicResourceService.listFiles(dd.folder() + ":0").stream().map((it) -> {
            return (RabbitMqDataSourceInfo)it;
        }).filter((it) -> {
            return !it.getId().equals(entity.getId());
        }).noneMatch((it) -> {
            return Objects.equals(it.getKey(), entity.getKey());
        });
        this.isTrue(noneMatchKey, DS_KEY_CONFLICT);

        // 验证数据源key的不可修改性
        if (entity.getId() != null) {
            RabbitMqDataSourceInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
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

    /**
     * 获取配置映射表
     *
     * @return 返回MagicRabbitMqConfig配置类的实例，该实例包含了RabbitMQ的相关配置信息
     */
    default Map<String, Object> getConfigMap() {
        return SpringUtils.getBean(MagicRabbitMqConfig.class) ;
    }


}
