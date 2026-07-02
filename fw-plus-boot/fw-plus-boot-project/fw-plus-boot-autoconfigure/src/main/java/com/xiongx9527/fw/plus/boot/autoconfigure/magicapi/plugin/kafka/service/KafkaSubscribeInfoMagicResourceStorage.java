package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service;

import com.alibaba.fastjson.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.config.KafkaCodeConstants.*;
import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.config.PulsarCodeConstants.PULSAR_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT;

/**
 * @author zzy
 */
public class KafkaSubscribeInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<KafkaSubscribeInfo> {

    /**
     * 获取文件夹名称
     *
     * @return 返回固定字符串"kafka-subscribe"，表示Kafka订阅相关的文件夹名称
     */
    @Override
    public String folder() {
        return "kafka-subscribe";
    }

    /**
     * 判断是否需要路径信息
     *
     * @return 返回false，表示该操作不需要额外的路径信息
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 获取魔法类的类型信息
     *
     * @return 返回KafkaSubscribeInfo.class，表示与Kafka订阅信息相关的类类型
     */
    @Override
    public Class<KafkaSubscribeInfo> magicClass() {
        return KafkaSubscribeInfo.class;
    }

    /**
     * 查找并返回所有Kafka数据源信息
     *
     * @return 包含所有Kafka数据源信息的列表
     */
    public List<KafkaSubscribeInfo> findAll() {
        return magicResourceService.files(folder() );
    }

    /**
     * 验证Kafka订阅信息的合法性
     * 此方法主要负责检查Kafka订阅信息中的关键字段是否为空，以及是否违反了唯一性约束
     * 它首先确保订阅的key和topic不为空，然后检查在当前文件夹下是否存在重复的订阅信息
     * 最后，如果订阅信息中包含其他字段，则验证其格式必须为JSON对象
     *
     * @param entity Kafka订阅信息实体，包含订阅的详细信息
     */
    @Override
    public void validate(KafkaSubscribeInfo entity) {
        // 检查Kafka订阅的key是否为空，如果为空，则抛出异常
        notBlank(entity.getKey(), KAFKA_SUBSCRIBE_KEY_REQUIRED);
        // 检查Kafka订阅的topic是否为空，如果为空，则抛出异常
        notBlank(entity.getTopic(), KAFKA_SUBSCRIBE_TOPIC_REQUIRED);
        // 获取当前文件夹下所有的Magic实体文件
        List<MagicEntity> files = magicResourceService.files(folder());
        // 检查是否存在重复的订阅信息，排除自身ID相同的情况
        boolean exists = files.stream().filter(x -> !x.getId().equals(entity.getId())).anyMatch(x -> {
            KafkaSubscribeInfo info = (KafkaSubscribeInfo) x;
            String key = info.getKey();
            String queue = info.getTopic();
            // 判断key和topic是否与当前实体相同
            return key.equals(entity.getKey()) && queue.equals(entity.getTopic());
        });
        // 如果存在重复的订阅信息，则抛出异常
        isTrue(!exists, KAFKA_SUBSCRIBE_KEY_TOPIC_REPEAT);
        // 如果其他字段不为空，则验证其格式
        if(StringUtils.isNotBlank(entity.getOther())) {
            // 创建JSON验证器，用于验证其他字段的格式
            JSONValidator validator = JSONValidator.from(entity.getOther());
            // 验证其他字段必须是有效的JSON对象
            isTrue(validator.validate() && validator.getType() == JSONValidator.Type.Object,
                    KAFKA_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT);
        }
        KafkaDataSourceInfoMagicResourceStorage kafkaDataSourceInfoMagicResourceStorage = SpringUtils.getBean(KafkaDataSourceInfoMagicResourceStorage.class);
        Map<String,KafkaDataSourceInfo> kafkaDataSourceInfoMap = ListUtils.emptyIfNull(kafkaDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(KafkaDataSourceInfo::getKey, val->val,(k1, k2)->k1));
        if (!kafkaDataSourceInfoMap.containsKey(entity.getKey())) {
            throw new IllegalArgumentException(String.format("数据源key:%s 不存在", entity.getKey()));
        }
    }

    /**
     * 重写构建映射键的方法
     * 此方法是根据Kafka订阅信息来构建一个唯一的映射键
     * 主要用于在Kafka消费者订阅主题时，生成对应的映射键，以便进行后续的处理
     *
     * @param info Kafka订阅信息，包含构建映射键所需的信息
     * @return 返回构建好的映射键字符串
     */
    @Override
    public String buildMappingKey(KafkaSubscribeInfo info) {
        // 调用重载方法，传入Kafka订阅信息和魔术资源服务获取的组路径
        // 组路径是根据订阅信息中的组ID获取的
        return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
    }
}
