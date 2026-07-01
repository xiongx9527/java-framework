package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service;

import com.alibaba.fastjson.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeInfo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.config.KafkaCodeConstants.KAFKA_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT;
import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.config.RabbitmqJsonCodeConstants.*;

public class RabbitMqSubscribeInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<RabbitMqSubscribeInfo> {

    /**
     * RabbitMqSubscribeInfoMagicResourceStorage构造函数
     *
     * 无参构造函数，用于创建RabbitMqSubscribeInfoMagicResourceStorage实例
     */
    public RabbitMqSubscribeInfoMagicResourceStorage() {
    }

    /**
     * 获取文件夹名称
     * @return 返回RabbitMQ订阅配置文件夹名称 "rabbitmq-subscribe"
     */
    public String folder() {
        return "rabbitmq-subscribe";
    }

    /**
     * 检查是否需要路径信息
     *
     * @return boolean 返回false，表示不需要路径信息
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 获取魔法类的类型
     * @return 返回RabbitMqSubscribeInfo类对象
     */
    public Class<RabbitMqSubscribeInfo> magicClass() {
        return RabbitMqSubscribeInfo.class;
    }

    /**
     * 查找所有RabbitMQ订阅信息
     *
     * @return 包含所有RabbitMQ订阅信息的列表
     */
    public List<RabbitMqSubscribeInfo> findAll() {
        return this.magicResourceService.files(folder() );
    }


    /**
     * 验证RabbitMQ订阅信息实体的有效性
     * @param entity 待验证的RabbitMQ订阅信息实体
     */
    public void validate(RabbitMqSubscribeInfo entity) {
        // 验证关键字段不为空
        this.notBlank(entity.getKey(), RABBITMQ_SUBSCRIBE_KEY_REQUIRED);
        this.notBlank(entity.getQueueName(), RABBITMQ_SUBSCRIBE_QUEUE_REQUIRED);

        // 检查是否存在重复的key和queue组合
        List<MagicEntity> files = this.magicResourceService.files(this.folder());
        boolean exists = files.stream().filter((x) -> {
            return !x.getId().equals(entity.getId());
        }).anyMatch((x) -> {
            RabbitMqSubscribeInfo info = (RabbitMqSubscribeInfo)x;
            String key = info.getKey();
            String queue = info.getQueueName();
            return key.equals(entity.getKey()) && queue.equals(entity.getQueueName());
        });
        this.isTrue(!exists, RABBITMQ_SUBSCRIBE_KEY_QUEUE_REPEAT);
        // 如果其他字段不为空，则验证其格式
        if(StringUtils.isNotBlank(entity.getOther())) {
            // 创建JSON验证器，用于验证其他字段的格式
            JSONValidator validator = JSONValidator.from(entity.getOther());
            // 验证其他字段必须是有效的JSON对象
            isTrue(validator.validate() && validator.getType() == JSONValidator.Type.Object,
                    KAFKA_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT);
        }
        RabbitMqDataSourceInfoMagicResourceStorage rabbitMqDataSourceInfoMagicResourceStorage = SpringUtils.getBean(RabbitMqDataSourceInfoMagicResourceStorage.class);
        Map<String, RabbitMqDataSourceInfo> rabbitMqDataSourceInfoMap = ListUtils.emptyIfNull(rabbitMqDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(RabbitMqDataSourceInfo::getKey, val->val,(k1, k2)->k1));
        if (!rabbitMqDataSourceInfoMap.containsKey(entity.getKey())) {
            throw new IllegalArgumentException(String.format("数据源key:%s 不存在", entity.getKey()));
        }
    }

    /**
     * 构建映射键
     * @param info RabbitMQ订阅信息对象
     * @return 返回构建的映射键字符串
     */
    public String buildMappingKey(RabbitMqSubscribeInfo info) {
        return this.buildMappingKey(info, this.magicResourceService.getGroupPath(info.getGroupId()));
    }


}
