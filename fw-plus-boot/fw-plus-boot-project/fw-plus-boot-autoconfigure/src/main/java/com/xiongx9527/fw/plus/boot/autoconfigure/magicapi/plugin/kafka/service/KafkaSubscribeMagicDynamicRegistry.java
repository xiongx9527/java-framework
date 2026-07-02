package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeScriptInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.MagicDynamicKafkaDataSource;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.springframework.context.event.EventListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Kafka 订阅 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class KafkaSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<KafkaSubscribeInfo> {
    /**
     * 魔法动态Kafka数据源的私有最终字段，用于在Kafka订阅魔法动态注册中处理数据源的动态配置
     */
    private final MagicDynamicKafkaDataSource magicDynamicKafkaDataSource;

    /**
     * 构造函数，用于初始化Kafka订阅魔法动态注册
     *
     * @param magicResourceStorage 存储Kafka订阅信息的魔法资源存储对象，用于管理订阅资源
     * @param magicDynamicKafkaDataSource 魔法动态Kafka数据源对象，负责处理数据源的动态配置
     */
    public KafkaSubscribeMagicDynamicRegistry(MagicResourceStorage<KafkaSubscribeInfo> magicResourceStorage,
                                              MagicDynamicKafkaDataSource magicDynamicKafkaDataSource) {
        // 调用父类构造函数初始化魔法资源存储
        super(magicResourceStorage);
        // 初始化魔法动态Kafka数据源
        this.magicDynamicKafkaDataSource = magicDynamicKafkaDataSource;
    }

    /**
     * 监听文件事件，仅当事件类型为"kafka-subscribe"时触发
     *
     * @param event 文件事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'kafka-subscribe'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("处理文件事件时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听组事件，仅当事件类型为"kafka-subscribe"时触发
     *
     * @param event 组事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'kafka-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }

    /**
     * 根据实体取消订阅Kafka消息
     *
     * @param entity Kafka订阅信息实体
     */
    private void unregisterByEntity(KafkaSubscribeInfo entity) {
        KafkaConsumer consumer = this.magicDynamicKafkaDataSource.getConsumer(entity.getKey());
        // 如果获取到的 consumer 不为空，则调用其 unsubscribe 方法取消订阅指定的 info
        if (Objects.nonNull(consumer)) {
            consumer.unsubscribe(entity);
        }
    }

    /**
     * 根据实体注册Kafka订阅信息
     *
     * @param entity Kafka订阅信息实体
     */
    private void registerByEntity(KafkaSubscribeInfo entity) {
        // 创建MagicResourceService实例
        MagicResourceService resourceService = MagicConfiguration.getMagicResourceService();
        // 获取相同Key的所有订阅，添加到消费实例中
        String scriptName = resourceService.getScriptName(entity);
        unregisterByEntity(entity);
        // 如果订阅是启用状态，则进行注册
        if (Boolean.TRUE.equals(entity.isEnabled())) {
            try {
                log.debug("Kafka 消息队列订阅:[{}]开始执行", scriptName);
                KafkaDataSourceInfoMagicResourceStorage kafkaDataSourceInfoMagicResourceStorage = SpringUtils.getBean(KafkaDataSourceInfoMagicResourceStorage.class);
                Map<String, KafkaDataSourceInfo> kafkaDataSourceInfoMap = ListUtils.emptyIfNull(kafkaDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(KafkaDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                if(!kafkaDataSourceInfoMap.containsKey(entity.getKey())) {
                    Thread.sleep(5000);
                    kafkaDataSourceInfoMap = ListUtils.emptyIfNull(kafkaDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(KafkaDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                    if(!kafkaDataSourceInfoMap.containsKey(entity.getKey())) {
                        log.warn("Kafka 订阅:[{}]执行出错, 未找到数据源[{}]", scriptName, entity.getKey());
                        return ;
                    }
                }
                List<KafkaSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                // 添加新的订阅信息
                newSubscribeList.add(new KafkaSubscribeScriptInfo(entity, scriptName));
                log.info("Kafka 订阅:[{}]开始执行========================================-=-=-=-==--=-=-=-==", scriptName);
                magicDynamicKafkaDataSource.putConsumer(kafkaDataSourceInfoMap.get(entity.getKey()), newSubscribeList);
                log.debug("Kafka 消息队列订阅:[{}]执行完毕", scriptName);
            } catch (Exception e) {
                log.error("Kafka 消息队列订阅:[{}]执行出错, {}", scriptName, e);
            }
        }
    }

    /**
     * 注册Kafka订阅信息，首先执行注销操作，然后调用父类的注册方法
     *
     * @param entity Kafka订阅信息实体，包含需要注册的信息
     * @return boolean 表示注册是否成功
     */
    @Override
    public boolean register(KafkaSubscribeInfo entity) {
        registerByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 注册Kafka订阅信息
     *
     * @param mappingNode 包含Kafka订阅信息的映射节点
     * @return 总是返回true，表示注册操作已完成
     */
    @Override
    protected boolean register(MappingNode<KafkaSubscribeInfo> mappingNode) {
        //KafkaSubscribeInfo entity = mappingNode.getEntity();
        //registerByEntity(entity);
        return super.register(mappingNode);
    }

    /**
     * 重写 unregister 方法以取消注册 Kafka 订阅信息
     *
     * @param mappingNode 包含 Kafka 订阅信息的映射节点
     */
    @Override
    protected void unregister(MappingNode<KafkaSubscribeInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        super.unregister(mappingNode);
    }
}
