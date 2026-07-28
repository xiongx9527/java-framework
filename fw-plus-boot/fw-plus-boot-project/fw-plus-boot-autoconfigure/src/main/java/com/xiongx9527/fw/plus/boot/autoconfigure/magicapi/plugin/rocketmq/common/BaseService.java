package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service.RocketMqDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.IoUtils;

import java.util.Objects;

import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.config.RocketmqJsonCodeConstants.ROCKETMQ_DATASOURCE_NAMESERVER_REQUIRED;

public interface BaseService extends MagicBaseService, JsonCodeConstants {


    /**
     * 检查RocketMQ订阅是否启用
     * <p>
     * 该方法首先从实体对象中获取启用状态，然后尝试从配置中心获取对应的配置值进行覆盖。
     * 如果配置获取失败，则默认返回false。
     * </p>
     *
     * @param entity RocketMQ订阅信息实体，包含订阅的Key、Topic等配置信息
     * @return boolean 订阅是否启用，true表示启用，false表示禁用
     */
    default boolean enable(RocketMqSubscribeInfo entity) {
        Boolean enable = entity.isEnabled();
        String enableStr = "false";
        if (enable != null) {
            enableStr = enable.toString();
        }
        try {
            enableStr = MagicAPIConfigUtils.getRocketMQSubscribeConfig(entity.getKey(), entity.getTopic(), "enabled", enableStr);
            enable = Boolean.parseBoolean(enableStr);
        } catch (Exception e) {
            enable = Boolean.FALSE;
        }
        return enable;
    }

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

    }
}
