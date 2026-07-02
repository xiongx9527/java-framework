package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.RocketMQConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeScriptInfo;
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
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.MagicDynamicRocketMqDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RocketMq 订阅 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class RocketMqSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<RocketMqSubscribeInfo> {

    /**
     * MagicDynamicRocketMqDataSource实例，用于动态管理RocketMQ数据源
     * 该字段被声明为private final，确保线程安全性和不可变性
     */
    private final MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource;

    /**
     * RocketMQ订阅动态注册器构造函数
     *
     * @param magicResourceStorage 魔法资源配置存储器，用于存储RocketMQ订阅信息
     * @param magicDynamicRocketMqDataSource 动态RocketMQ数据源，用于获取RocketMQ连接配置
     */
    public RocketMqSubscribeMagicDynamicRegistry(MagicResourceStorage<RocketMqSubscribeInfo> magicResourceStorage,
                                                 MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource) {
        super(magicResourceStorage);
        this.magicDynamicRocketMqDataSource = magicDynamicRocketMqDataSource;
    }

    /**
     * 处理文件事件的监听器
     * 当事件类型为'rocketmq-subscribe'时触发，用于处理RocketMQ订阅相关的文件变更事件
     *
     * @param event 文件事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'rocketmq-subscribe'")
    public void onFileEvent(FileEvent event) {
        processEvent(event);
    }

    /**
     * 处理组事件的监听器
     * 当事件类型为'rocketmq-subscribe'时触发，用于处理RocketMQ订阅相关的组变更事件
     *
     * @param event 组事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'rocketmq-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }

    /**
     * 根据订阅实体取消注册对应的 RocketMQ 消费者。
     *
     * @param entity 订阅信息实体，用于获取消费者 key 并执行取消订阅操作
     */
    private void unregisterByEntity(RocketMqSubscribeInfo entity) {
        RocketMQConsumer consumer = this.magicDynamicRocketMqDataSource.getConsumer(entity.getKey());
        if (!Objects.isNull(consumer)) {
            consumer.unsubscribe(entity);
        }
    }

    /**
     * 根据订阅实体注册 RocketMQ 消费者。
     * <p>
     * 若订阅处于启用状态，则会尝试构建并注册一个新的消费者实例。在注册前，
     * 先调用 {@link #unregisterByEntity(RocketMqSubscribeInfo)} 取消已有的订阅，
     * 防止重复消费。若找不到对应的数据源配置，则记录警告日志并终止流程。
     *
     * @param entity 订阅信息实体，包含订阅配置及是否启用等信息
     */
    private void registerByEntity(RocketMqSubscribeInfo entity) {
        MagicResourceService resourceService = MagicConfiguration.getMagicResourceService();
        String scriptName = resourceService.getScriptName(entity);
        unregisterByEntity(entity);
        // 如果订阅是启用状态，则进行注册
        if (Boolean.TRUE.equals(entity.isEnabled())) {
            try {
                log.debug("RocketMq 消息队列订阅:[{}]开始执行", scriptName);
                RocketMqDataSourceInfoMagicResourceStorage rocketMqDataSourceInfoMagicResourceStorage = SpringUtils.getBean(RocketMqDataSourceInfoMagicResourceStorage.class);
                Map<String, RocketMqDataSourceInfo> rocketMqDataSourceInfoMap = ListUtils.emptyIfNull(rocketMqDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(RocketMqDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                // 尝试加载数据源信息，最多重试一次（等待5秒后再次查询）
                if(!rocketMqDataSourceInfoMap.containsKey(entity.getKey())) {
                    Thread.sleep(5000);
                    rocketMqDataSourceInfoMap = ListUtils.emptyIfNull(rocketMqDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(RocketMqDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                    if(!rocketMqDataSourceInfoMap.containsKey(entity.getKey())) {
                        log.warn("RocketMq 订阅:[{}]执行出错, 未找到数据源[{}]", scriptName, entity.getKey());
                        return ;
                    }
                }
                List<RocketMqSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                // 添加新的订阅信息
                newSubscribeList.add(new RocketMqSubscribeScriptInfo(entity, scriptName));
                this.magicDynamicRocketMqDataSource.putConsumer(rocketMqDataSourceInfoMap.get(entity.getKey()), newSubscribeList);
            } catch (Exception e) {
                log.error("RocketMq 消息队列订阅:[{}]执行出错, {}", scriptName, e);
            }
        }
    }


    /**
     * 注册RocketMQ订阅信息
     *
     * @param entity RocketMQ订阅信息实体对象
     * @return 注册成功返回true，失败返回false
     */
    @Override
    public boolean register(RocketMqSubscribeInfo entity) {
        // 先通过实体进行注册
        registerByEntity(entity);
        try {
            // 调用父类的注册方法
            return super.register(entity);
        } catch (Exception e) {
            // 发生异常时返回false
            return false;
        }
    }

    /**
     * 注册映射节点
     *
     * @param mappingNode RocketMQ订阅信息的映射节点
     * @return 注册成功返回true，失败返回false
     */
    @Override
    protected boolean register(MappingNode<RocketMqSubscribeInfo> mappingNode) {
        // 调用父类的注册方法
        return super.register(mappingNode);
    }

    /**
     * 取消注册映射节点
     *
     * @param mappingNode RocketMQ订阅信息的映射节点
     */
    @Override
    protected void unregister(MappingNode<RocketMqSubscribeInfo> mappingNode) {
        // 先通过实体取消注册
        unregisterByEntity(mappingNode.getEntity());
        // 调用父类的取消注册方法
        super.unregister(mappingNode);
    }


}
