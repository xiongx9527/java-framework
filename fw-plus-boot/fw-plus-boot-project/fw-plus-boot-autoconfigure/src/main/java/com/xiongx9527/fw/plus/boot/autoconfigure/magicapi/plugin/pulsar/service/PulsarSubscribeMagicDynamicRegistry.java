package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.PulsarConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.MagicDynamicPulsarDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Kafka 订阅 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class PulsarSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<PulsarSubscribeInfo> {

    /**
     * MagicDynamicPulsarDataSource实例，用于管理动态Pulsar数据源
     * 该字段被声明为private final，确保线程安全性和不可变性
     */
    private final MagicDynamicPulsarDataSource magicDynamicPulsarDataSource;


    /**
     * 构造函数，用于创建Pulsar订阅魔法动态注册器实例
     * @param magicResourceStorage 魔法资源存储器，用于存储Pulsar订阅信息
     * @param magicDynamicPulsarDataSource 魔法动态Pulsar数据源，用于管理Pulsar连接和订阅
     */
    public PulsarSubscribeMagicDynamicRegistry(MagicResourceStorage<PulsarSubscribeInfo> magicResourceStorage,
                                               MagicDynamicPulsarDataSource magicDynamicPulsarDataSource) {
        super(magicResourceStorage);
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
    }

    /**
     * 处理文件事件的监听器方法，当事件类型为'pulsar-subscribe'时触发
     * @param event 文件事件对象，包含事件相关的信息
     */
    @EventListener(condition = "#event.type == 'pulsar-subscribe'")
    public void onFileEvent(FileEvent event) {
        try {
            // 处理文件事件
            processEvent(event);
        } catch (Exception e) {
            log.error("处理文件事件时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理组事件的监听器方法，当事件类型为'pulsar-subscribe'时触发
     * @param event 组事件对象，包含事件相关的信息
     */
    @EventListener(condition = "#event.type == 'pulsar-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        // 处理组事件
        processEvent(event);
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
        if (Boolean.TRUE.equals(entity.isEnabled())) {
            try {
                log.info("Pulsar 消息队列订阅:[{}]开始执行", scriptName);
                PulsarDataSourceInfoMagicResourceStorage pulsarDataSourceInfoMagicResourceStorage = SpringUtils.getBean(PulsarDataSourceInfoMagicResourceStorage.class);
                Map<String, PulsarDataSourceInfo> pulsarDataSourceInfoMap = ListUtils.emptyIfNull(pulsarDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(PulsarDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                if(!pulsarDataSourceInfoMap.containsKey(entity.getKey())) {
                    Thread.sleep(5000);
                    pulsarDataSourceInfoMap = ListUtils.emptyIfNull(pulsarDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(PulsarDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                    if(!pulsarDataSourceInfoMap.containsKey(entity.getKey())) {
                        log.warn("Pulsar 订阅:[{}]执行出错, 未找到数据源[{}]", scriptName, entity.getKey());
                        return ;
                    }
                }
                List<PulsarSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                // 添加新的订阅信息
                newSubscribeList.add(new PulsarSubscribeScriptInfo(entity, scriptName));
                magicDynamicPulsarDataSource.putConsumer(pulsarDataSourceInfoMap.get(entity.getKey()), newSubscribeList);
            } catch (Exception e) {
                log.error("pulsar 消息队列订阅:[{}]执行出错, {}", scriptName, e.getMessage());
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
        } catch (Exception e) {
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
        //PulsarSubscribeInfo entity = mappingNode.getEntity();
        //PulsarConsumer consumer = this.magicDynamicPulsarDataSource.getConsumer(entity.getKey());
        //if (Objects.isNull(consumer)) {
        //    registerByEntity(mappingNode.getEntity());
        //}
        return super.register(mappingNode);
    }

    /**
     * 取消注册Pulsar订阅信息
     *
     * @param mappingNode 包含Pulsar订阅信息的映射节点
     */
    @Override
    protected void unregister(MappingNode<PulsarSubscribeInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        super.unregister(mappingNode);
    }

}
