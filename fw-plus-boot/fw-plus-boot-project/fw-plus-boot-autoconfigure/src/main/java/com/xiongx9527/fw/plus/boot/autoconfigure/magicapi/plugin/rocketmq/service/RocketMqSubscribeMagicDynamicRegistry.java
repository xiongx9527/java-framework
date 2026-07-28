package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.RocketMQConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.MagicDynamicRocketMqDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeScriptInfo;
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
 * RocketMq 订阅 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class RocketMqSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<RocketMqSubscribeInfo> implements BaseService {

    private final MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource;
    private final RocketMqDataSourceInfoMagicResourceStorage dataSourceStorage;
    private final RocketMqSubscribeInfoMagicResourceStorage subscribeStorage;
    private final Set<String> pendingSubscribeKeys = ConcurrentHashMap.newKeySet();

    public RocketMqSubscribeMagicDynamicRegistry(MagicResourceStorage<RocketMqSubscribeInfo> magicResourceStorage,
                                                 MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource,
                                                 RocketMqDataSourceInfoMagicResourceStorage dataSourceStorage,
                                                 RocketMqSubscribeInfoMagicResourceStorage subscribeStorage) {
        super(magicResourceStorage);
        this.magicDynamicRocketMqDataSource = magicDynamicRocketMqDataSource;
        this.dataSourceStorage = dataSourceStorage;
        this.subscribeStorage = subscribeStorage;
    }

    @EventListener(condition = "#event.type == 'rocketmq-subscribe'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @EventListener(condition = "#event.type == 'rocketmq-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听数据源事件，当数据源就绪后重新注册待重试的订阅
     */
    @EventListener(condition = "#event.type == 'rocketmq-datasource'")
    public void onDataSourceFileEvent(FileEvent event) {
        if (event.getAction() == EventAction.DELETE) {
            if (!(event.getEntity() instanceof RocketMqDataSourceInfo)) {
                return;
            }
            RocketMqDataSourceInfo entity = (RocketMqDataSourceInfo) event.getEntity();
            if (entity != null && entity.getKey() != null) {
                this.pendingSubscribeKeys.remove(entity.getKey());
            }
            return;
        }
        if (this.pendingSubscribeKeys.isEmpty()) {
            return;
        }
        try {
            Map<String, RocketMqDataSourceInfo> dsMap = ListUtils.emptyIfNull(this.dataSourceStorage.findAll()).stream()
                    .collect(Collectors.toMap(RocketMqDataSourceInfo::getKey, v -> v, (k1, k2) -> k1));

            for (String dsKey : new HashSet<>(this.pendingSubscribeKeys)) {
                if (!dsMap.containsKey(dsKey)) {
                    continue;
                }
                List<RocketMqSubscribeInfo> subscribes = ListUtils.emptyIfNull(this.subscribeStorage.findAll()).stream()
                        .filter(s -> dsKey.equals(s.getKey()))
                        .collect(Collectors.toList());
                for (RocketMqSubscribeInfo subscribe : subscribes) {
                    registerByEntity(subscribe);
                }
                this.pendingSubscribeKeys.remove(dsKey);
            }
        } catch (Exception e) {
            log.error("处理数据源事件时发生错误", e);
        }
    }

    private void unregisterByEntity(RocketMqSubscribeInfo entity) {
        RocketMQConsumer consumer = this.magicDynamicRocketMqDataSource.getConsumer(entity.getKey());
        if (!Objects.isNull(consumer)) {
            consumer.unsubscribe(entity);
        }
    }

    private void registerByEntity(RocketMqSubscribeInfo entity) {
        String scriptName = MagicConfiguration.getMagicResourceService().getScriptName(entity);
        unregisterByEntity(entity);
        if (enable(entity)) {
            try {
                log.debug("RocketMQ 消息队列订阅:[{}]开始执行", scriptName);
                Map<String, RocketMqDataSourceInfo> rocketMqDataSourceInfoMap = ListUtils.emptyIfNull(this.dataSourceStorage.findAll()).stream()
                        .collect(Collectors.toMap(RocketMqDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
                if (!rocketMqDataSourceInfoMap.containsKey(entity.getKey())) {
                    log.warn("RocketMQ 订阅:[{}]执行出错, 未找到数据源[{}], 加入待重试队列", scriptName, entity.getKey());
                    this.pendingSubscribeKeys.add(entity.getKey());
                    return;
                }
                List<RocketMqSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                newSubscribeList.add(new RocketMqSubscribeScriptInfo(entity, scriptName));
                this.magicDynamicRocketMqDataSource.putConsumer(rocketMqDataSourceInfoMap.get(entity.getKey()), newSubscribeList);
            } catch (Exception e) {
                log.error("RocketMQ 消息队列订阅:[{}]执行出错", scriptName, e);
            }
        }
    }

    @Override
    public boolean register(RocketMqSubscribeInfo entity) {
        registerByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<RocketMqSubscribeInfo> mappingNode) {
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<RocketMqSubscribeInfo> mappingNode) {
        RocketMqSubscribeInfo entity = mappingNode.getEntity();
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
