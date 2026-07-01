package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer.RabbitMQConsumer;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.MagicDynamicRabbitMqDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeScriptInfo;
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

@Slf4j
public class RabbitMqSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<RabbitMqSubscribeInfo> implements BaseService {

    private final MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource;
    private final RabbitMqDataSourceInfoMagicResourceStorage dataSourceStorage;
    private final RabbitMqSubscribeInfoMagicResourceStorage subscribeStorage;
    private final Set<String> pendingSubscribeKeys = ConcurrentHashMap.newKeySet();

    public RabbitMqSubscribeMagicDynamicRegistry(MagicResourceStorage<RabbitMqSubscribeInfo> magicResourceStorage,
                                                 MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource,
                                                 RabbitMqDataSourceInfoMagicResourceStorage dataSourceStorage,
                                                 RabbitMqSubscribeInfoMagicResourceStorage subscribeStorage) {
        super(magicResourceStorage);
        this.magicDynamicRabbitMqDataSource = magicDynamicRabbitMqDataSource;
        this.dataSourceStorage = dataSourceStorage;
        this.subscribeStorage = subscribeStorage;
    }


    @EventListener(condition = "#event.type == 'rabbitmq-subscribe'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @EventListener(condition = "#event.type == 'rabbitmq-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听数据源事件，当数据源就绪后重新注册待重试的订阅
     */
    @EventListener(condition = "#event.type == 'rabbitmq-datasource'")
    public void onDataSourceFileEvent(FileEvent event) {
        if (event.getAction() == EventAction.DELETE) {
            if (!(event.getEntity() instanceof RabbitMqDataSourceInfo)) {
                return;
            }
            RabbitMqDataSourceInfo entity = (RabbitMqDataSourceInfo) event.getEntity();
            if (entity != null && entity.getKey() != null) {
                this.pendingSubscribeKeys.remove(entity.getKey());
            }
            return;
        }
        if (this.pendingSubscribeKeys.isEmpty()) {
            return;
        }
        try {
            Map<String, RabbitMqDataSourceInfo> dsMap = ListUtils.emptyIfNull(this.dataSourceStorage.findAll()).stream()
                    .collect(Collectors.toMap(RabbitMqDataSourceInfo::getKey, v -> v, (k1, k2) -> k1));

            for (String dsKey : new HashSet<>(this.pendingSubscribeKeys)) {
                if (!dsMap.containsKey(dsKey)) {
                    continue;
                }
                List<RabbitMqSubscribeInfo> subscribes = ListUtils.emptyIfNull(this.subscribeStorage.findAll()).stream()
                        .filter(s -> dsKey.equals(s.getKey()))
                        .collect(Collectors.toList());
                for (RabbitMqSubscribeInfo subscribe : subscribes) {
                    registerByEntity(subscribe);
                }
                this.pendingSubscribeKeys.remove(dsKey);
            }
        } catch (Exception ignored) {
        }
    }

    private void unregisterByEntity(RabbitMqSubscribeInfo entity) {
        RabbitMQConsumer consumer = this.magicDynamicRabbitMqDataSource.getConsumer(entity.getKey());
        if (!Objects.isNull(consumer)) {
            consumer.unsubscribe(entity);
        }
    }

    private void registerByEntity(RabbitMqSubscribeInfo entity) {
        String scriptName = MagicConfiguration.getMagicResourceService().getScriptName(entity);
        unregisterByEntity(entity);
        if (enable(entity)) {
            try {
                log.debug("RabbitMQ 消息队列订阅:[{}]开始执行", scriptName);
                Map<String, RabbitMqDataSourceInfo> rabbitMqDataSourceInfoMap = ListUtils.emptyIfNull(this.dataSourceStorage.findAll()).stream()
                        .collect(Collectors.toMap(RabbitMqDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
                if (!rabbitMqDataSourceInfoMap.containsKey(entity.getKey())) {
                    log.warn("RabbitMQ 订阅:[{}]执行出错, 未找到数据源[{}], 加入待重试队列", scriptName, entity.getKey());
                    this.pendingSubscribeKeys.add(entity.getKey());
                    return;
                }
                List<RabbitMqSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                newSubscribeList.add(new RabbitMqSubscribeScriptInfo(entity, scriptName));
                this.magicDynamicRabbitMqDataSource.putConsumer(rabbitMqDataSourceInfoMap.get(entity.getKey()), newSubscribeList);
            } catch (Exception e) {
                log.error("RabbitMQ 消息队列订阅:[{}]执行出错, {}", scriptName, e);
            }
        }
    }

    @Override
    public boolean register(RabbitMqSubscribeInfo entity) {
        registerByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<RabbitMqSubscribeInfo> mappingNode) {
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<RabbitMqSubscribeInfo> mappingNode) {
        RabbitMqSubscribeInfo entity = mappingNode.getEntity();
        unregisterByEntity(entity);
        // TODO: findAll() 全量扫描效率低，大数据量时应改为按 key 索引查找
        boolean hasOther = ListUtils.emptyIfNull(this.subscribeStorage.findAll()).stream()
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
