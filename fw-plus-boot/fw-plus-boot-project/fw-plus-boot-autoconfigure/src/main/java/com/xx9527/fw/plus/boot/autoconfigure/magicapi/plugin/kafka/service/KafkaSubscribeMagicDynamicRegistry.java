package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumer;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeScriptInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.MagicDynamicKafkaDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Kafka 订阅 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class KafkaSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<KafkaSubscribeInfo> implements BaseService {
    private final MagicDynamicKafkaDataSource magicDynamicKafkaDataSource;
    private final KafkaDataSourceInfoMagicResourceStorage kafkaDataSourceStorage;
    private final KafkaSubscribeInfoMagicResourceStorage kafkaSubscribeStorage;
    private final Set<String> pendingSubscribeKeys = ConcurrentHashMap.newKeySet();

    public KafkaSubscribeMagicDynamicRegistry(MagicResourceStorage<KafkaSubscribeInfo> magicResourceStorage,
                                              MagicDynamicKafkaDataSource magicDynamicKafkaDataSource,
                                              KafkaDataSourceInfoMagicResourceStorage kafkaDataSourceStorage,
                                              KafkaSubscribeInfoMagicResourceStorage kafkaSubscribeStorage) {
        super(magicResourceStorage);
        this.magicDynamicKafkaDataSource = magicDynamicKafkaDataSource;
        this.kafkaDataSourceStorage = kafkaDataSourceStorage;
        this.kafkaSubscribeStorage = kafkaSubscribeStorage;
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
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听组事件，仅当事件类型为"kafka-subscribe"时触发
     *
     * @param event 组事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'kafka-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
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
        String scriptName = MagicConfiguration.getMagicResourceService().getScriptName(entity);
        unregisterByEntity(entity);
        // 如果订阅是启用状态，则进行注册
        if (enable(entity)) {
            try {
                log.debug("Kafka 消息队列订阅:[{}]开始执行", scriptName);
                Map<String, KafkaDataSourceInfo> kafkaDataSourceInfoMap = ListUtils.emptyIfNull(kafkaDataSourceStorage.findAll()).stream().collect(Collectors.toMap(KafkaDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
                if(!kafkaDataSourceInfoMap.containsKey(entity.getKey())) {
                    log.warn("Kafka 订阅:[{}]执行出错, 未找到数据源[{}], 加入待重试队列", scriptName, entity.getKey());
                    this.pendingSubscribeKeys.add(entity.getKey());
                    return ;
                }
                List<KafkaSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                // 添加新的订阅信息
                newSubscribeList.add(new KafkaSubscribeScriptInfo(entity, scriptName));
                magicDynamicKafkaDataSource.putConsumer(kafkaDataSourceInfoMap.get(entity.getKey()), newSubscribeList);
                log.debug("Kafka 消息队列订阅:[{}]执行完毕", scriptName);
            } catch (Exception e) {
                log.error("Kafka 消息队列订阅:[{}]执行出错", scriptName, e);
            }
        }
    }

    /**
     * 注册Kafka订阅信息
     *
     * @param entity Kafka订阅信息实体
     * @return boolean 注册是否成功
     */
    @Override
    public boolean register(KafkaSubscribeInfo entity) {
        registerByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 监听数据源事件，当数据源就绪后重新注册待重试的订阅
     */
    @EventListener(condition = "#event.type == 'kafka-datasource'")
    public void onDataSourceFileEvent(FileEvent event) {
        // 如果是删除事件，清理待重试队列中的对应 key
        if (event.getAction() == EventAction.DELETE) {
            if (!(event.getEntity() instanceof KafkaDataSourceInfo)) {
                return;
            }
            KafkaDataSourceInfo entity = (KafkaDataSourceInfo) event.getEntity();
            if (entity != null && entity.getKey() != null) {
                this.pendingSubscribeKeys.remove(entity.getKey());
            }
            return;
        }
        if (this.pendingSubscribeKeys.isEmpty()) {
            return;
        }
        try {
            Map<String, KafkaDataSourceInfo> dsMap = ListUtils.emptyIfNull(kafkaDataSourceStorage.findAll()).stream()
                    .collect(Collectors.toMap(KafkaDataSourceInfo::getKey, v -> v, (k1, k2) -> k1));

            for (String dsKey : new HashSet<>(this.pendingSubscribeKeys)) {
                if (!dsMap.containsKey(dsKey)) {
                    continue;
                }
                List<KafkaSubscribeInfo> subscribes = ListUtils.emptyIfNull(kafkaSubscribeStorage.findAll()).stream()
                        .filter(s -> dsKey.equals(s.getKey()))
                        .collect(Collectors.toList());
                for (KafkaSubscribeInfo subscribe : subscribes) {
                    registerByEntity(subscribe);
                }
                this.pendingSubscribeKeys.remove(dsKey);
            }
        } catch (Exception e) {
            log.error("处理数据源事件时发生错误", e);
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
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 取消注册Kafka订阅信息
     *
     * @param mappingNode 包含Kafka订阅信息的映射节点
     */
    @Override
    protected void unregister(MappingNode<KafkaSubscribeInfo> mappingNode) {
        KafkaSubscribeInfo entity = mappingNode.getEntity();
        unregisterByEntity(entity);
        // TODO: findAll() 全量扫描效率低，大数据量时应改为按 key 索引查找
        boolean hasOther = ListUtils.emptyIfNull(kafkaSubscribeStorage.findAll()).stream()
                .anyMatch(s -> entity.getKey().equals(s.getKey()) && !s.getId().equals(entity.getId()));
        if (!hasOther) {
            this.pendingSubscribeKeys.remove(entity.getKey());
        }
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }
}
