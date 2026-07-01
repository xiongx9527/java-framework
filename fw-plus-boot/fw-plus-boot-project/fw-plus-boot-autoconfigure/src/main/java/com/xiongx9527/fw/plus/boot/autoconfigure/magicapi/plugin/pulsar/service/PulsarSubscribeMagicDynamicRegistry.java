package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.PulsarConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.MagicDynamicPulsarDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeScriptInfo;
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
 * Pulsar 订阅 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class PulsarSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<PulsarSubscribeInfo> implements BaseService {

    /**
     * MagicDynamicPulsarDataSource实例，用于管理动态Pulsar数据源
     * 该字段被声明为private final，确保线程安全性和不可变性
     */
    private final MagicDynamicPulsarDataSource magicDynamicPulsarDataSource;

    /**
     * 数据源信息资源存储，用于查询数据源是否存在
     */
    private final PulsarDataSourceInfoMagicResourceStorage dataSourceStorage;

    /**
     * 订阅信息资源存储，用于查询使用相同数据源的订阅
     */
    private final PulsarSubscribeInfoMagicResourceStorage subscribeStorage;

    /**
     * 因数据源未就绪而等待重试的订阅 key 集合
     */
    private final Set<String> pendingSubscribeKeys = ConcurrentHashMap.newKeySet();


    /**
     * 构造函数，用于创建Pulsar订阅魔法动态注册器实例
     * @param magicResourceStorage 魔法资源存储器，用于存储Pulsar订阅信息
     * @param magicDynamicPulsarDataSource 魔法动态Pulsar数据源，用于管理Pulsar连接和订阅
     * @param dataSourceStorage 数据源信息资源存储，用于查询数据源是否存在
     * @param subscribeStorage 订阅信息资源存储，用于查询使用相同数据源的订阅
     */
    public PulsarSubscribeMagicDynamicRegistry(MagicResourceStorage<PulsarSubscribeInfo> magicResourceStorage,
                                               MagicDynamicPulsarDataSource magicDynamicPulsarDataSource,
                                               PulsarDataSourceInfoMagicResourceStorage dataSourceStorage,
                                               PulsarSubscribeInfoMagicResourceStorage subscribeStorage) {
        super(magicResourceStorage);
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
        this.dataSourceStorage = dataSourceStorage;
        this.subscribeStorage = subscribeStorage;
    }

    /**
     * 处理文件事件的监听器方法，当事件类型为'pulsar-subscribe'时触发
     * @param event 文件事件对象，包含事件相关的信息
     */
    @EventListener(condition = "#event.type == 'pulsar-subscribe'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理组事件的监听器方法，当事件类型为'pulsar-subscribe'时触发
     * @param event 组事件对象，包含事件相关的信息
     */
    @EventListener(condition = "#event.type == 'pulsar-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听数据源事件，当数据源就绪后重新注册待重试的订阅
     */
    @EventListener(condition = "#event.type == 'pulsar-datasource'")
    public void onDataSourceFileEvent(FileEvent event) {
        // 如果是删除事件，清理待重试队列中的对应 key
        if (event.getAction() == EventAction.DELETE) {
            if (!(event.getEntity() instanceof PulsarDataSourceInfo)) {
                return;
            }
            PulsarDataSourceInfo entity = (PulsarDataSourceInfo) event.getEntity();
            if (entity != null && entity.getKey() != null) {
                this.pendingSubscribeKeys.remove(entity.getKey());
            }
            return;
        }
        if (this.pendingSubscribeKeys.isEmpty()) {
            return;
        }
        try {
            Map<String, PulsarDataSourceInfo> dsMap = ListUtils.emptyIfNull(this.dataSourceStorage.findAll()).stream()
                    .collect(Collectors.toMap(PulsarDataSourceInfo::getKey, v -> v, (k1, k2) -> k1));

            for (String dsKey : new HashSet<>(this.pendingSubscribeKeys)) {
                if (!dsMap.containsKey(dsKey)) {
                    continue;
                }
                List<PulsarSubscribeInfo> subscribes = ListUtils.emptyIfNull(this.subscribeStorage.findAll()).stream()
                        .filter(s -> dsKey.equals(s.getKey()))
                        .collect(Collectors.toList());
                for (PulsarSubscribeInfo subscribe : subscribes) {
                    registerByEntity(subscribe);
                }
                this.pendingSubscribeKeys.remove(dsKey);
            }
        } catch (Exception e) {
            log.error("处理数据源事件时发生错误", e);
        }
    }

    /**
     * 根据实体取消Pulsar订阅
     *
     * @param entity Pulsar订阅信息实体
     */
    private void unregisterByEntity(PulsarSubscribeInfo entity) {
        PulsarConsumer consumer = this.magicDynamicPulsarDataSource.getConsumer(entity.getKey());
        if (!Objects.isNull(consumer)) {
            consumer.unsubscribe(entity);
        }
    }

    /**
     * 根据实体注册Pulsar订阅信息
     *
     * @param entity Pulsar订阅信息实体
     */
    private void registerByEntity(PulsarSubscribeInfo entity) {
        // 获取相同Key的所有订阅，添加到消费实例中
        String scriptName = MagicConfiguration.getMagicResourceService().getScriptName(entity);
        unregisterByEntity(entity);
        // 如果订阅是启用状态，则进行注册
        if (enable(entity)) {
            try {
                log.info("Pulsar 消息队列订阅:[{}]开始执行", scriptName);
                Map<String, PulsarDataSourceInfo> pulsarDataSourceInfoMap = ListUtils.emptyIfNull(this.dataSourceStorage.findAll()).stream().collect(Collectors.toMap(PulsarDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                if(!pulsarDataSourceInfoMap.containsKey(entity.getKey())) {
                    log.warn("Pulsar 订阅:[{}]执行出错, 未找到数据源[{}], 加入待重试队列", scriptName, entity.getKey());
                    this.pendingSubscribeKeys.add(entity.getKey());
                    return ;
                }
                List<PulsarSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                newSubscribeList.add(new PulsarSubscribeScriptInfo(entity, scriptName));
                magicDynamicPulsarDataSource.putConsumer(pulsarDataSourceInfoMap.get(entity.getKey()), newSubscribeList);
            } catch (Exception e) {
                log.error("pulsar 消息队列订阅:[{}]执行出错", scriptName, e);
            }
        } 
    }

    /**
     * 注册Pulsar订阅信息
     *
     * @param entity Pulsar订阅信息实体
     * @return 注册是否成功
     */
    @Override
    public boolean register(PulsarSubscribeInfo entity) {
        registerByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册Pulsar订阅信息
     * 该方法处理Pulsar订阅的注册逻辑，包括启动或停止订阅消费实例
     *
     * @param mappingNode Pulsar订阅信息映射节点
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<PulsarSubscribeInfo> mappingNode) {
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 取消注册Pulsar订阅信息
     *
     * @param mappingNode 包含Pulsar订阅信息的映射节点
     */
    @Override
    protected void unregister(MappingNode<PulsarSubscribeInfo> mappingNode) {
        PulsarSubscribeInfo entity = mappingNode.getEntity();
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
