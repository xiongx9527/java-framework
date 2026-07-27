package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service.RabbitMqDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.starter.MagicRabbitMqConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.IoUtils;

import java.util.Map;
import java.util.Objects;

import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.config.RabbitmqJsonCodeConstants.RABBITMQ_DATASOURCE_HOST_REQUIRED;
import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.config.RabbitmqJsonCodeConstants.RABBITMQ_DATASOURCE_PORT_REQUIRED;

public interface BaseService extends MagicBaseService , JsonCodeConstants {


    /**
     * 检查RabbitMQ订阅是否启用
     * <p>
     * 该方法首先从实体对象中获取启用状态，然后尝试从配置中心获取对应的配置值进行覆盖。
     * 如果配置获取失败，则默认返回false。
     * </p>
     *
     * @param entity RabbitMQ订阅信息实体，包含订阅的Key、QueueName等配置信息
     * @return boolean 订阅是否启用，true表示启用，false表示禁用
     */
    default boolean enable(RabbitMqSubscribeInfo entity) {
        Boolean enable = entity.isEnabled();
        String enableStr = "false";
        if (enable != null) {
            enableStr = enable.toString();
        }
        try {
            enableStr = MagicAPIConfigUtils.getRabbitMQSubscribeConfig(entity.getKey(), entity.getQueueName(), "enabled", enableStr);
            enable = Boolean.parseBoolean(enableStr);
        } catch (Exception e) {
            enable = Boolean.FALSE;
        }
        return enable;
    }

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
