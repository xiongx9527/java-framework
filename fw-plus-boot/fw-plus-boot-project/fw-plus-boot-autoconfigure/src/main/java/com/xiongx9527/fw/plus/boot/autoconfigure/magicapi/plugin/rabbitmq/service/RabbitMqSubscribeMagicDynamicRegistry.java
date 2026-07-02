package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer.RabbitMQConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.MagicDynamicRabbitMqDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;

@Slf4j
public class RabbitMqSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<RabbitMqSubscribeInfo> {

    /**
     * RabbitMQ订阅动态注册器
     * 用于管理RabbitMQ订阅信息的动态注册和数据源配置
     */
    private final MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource;

    /**
     * 构造函数，初始化RabbitMQ订阅动态注册器
     * @param magicResourceStorage RabbitMQ订阅信息的资源存储器，用于存储和管理订阅配置信息
     * @param magicDynamicRabbitMqDataSource 动态RabbitMQ数据源，提供RabbitMQ连接和消息处理能力
     */
    public RabbitMqSubscribeMagicDynamicRegistry(MagicResourceStorage<RabbitMqSubscribeInfo> magicResourceStorage, MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource) {
        super(magicResourceStorage);
        this.magicDynamicRabbitMqDataSource = magicDynamicRabbitMqDataSource;
    }


    /**
     * 监听文件事件，当事件类型为 'rabbitmq-subscribe' 时触发。
     *
     * @param event 文件事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'rabbitmq-subscribe'")
    public void onFileEvent(FileEvent event) {
        this.processEvent(event);
    }

    /**
     * 监听组事件，当事件类型为 'rabbitmq-subscribe' 时触发。
     *
     * @param event 组事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'rabbitmq-subscribe'")
    public void onGroupEvent(GroupEvent event) {
        this.processEvent(event);
    }

    /**
     * 根据订阅信息实体取消注册消费者
     *
     * @param entity RabbitMQ订阅信息实体，包含取消订阅所需的关键信息
     */
    private void unregisterByEntity(RabbitMqSubscribeInfo entity) {
        // 获取指定key对应的RabbitMQ消费者实例
        RabbitMQConsumer consumer = this.magicDynamicRabbitMqDataSource.getConsumer(entity.getKey());
        // 如果消费者实例存在，则执行取消订阅操作
        if (!Objects.isNull(consumer)) {
            consumer.unsubscribe(entity);
        }
    }

    /**
     * 根据订阅实体信息注册 RabbitMQ 消费者。
     * <p>
     * 该方法会根据传入的 {@link RabbitMqSubscribeInfo} 实体对象，
     * 获取或创建对应的脚本名称，并根据其启用状态决定是否注册消费者。
     * 若已存在相同 key 和 queueName 的订阅信息，则先取消注册后再重新注册（如启用）。
     * </p>
     *
     * @param entity 订阅信息实体对象，包含订阅的关键配置信息，如 key、queueName、isEnabled 等。
     */
    private void registerByEntity(RabbitMqSubscribeInfo entity) {
        // 创建MagicResourceService实例
        MagicResourceService resourceService = MagicConfiguration.getMagicResourceService();
        // 获取相同Key的所有订阅，添加到消费实例中
        String scriptName = resourceService.getScriptName(entity);
        // 先注销当前实体可能存在的旧订阅
        unregisterByEntity(entity);

        // 如果订阅是启用状态，则进行注册
        if (Boolean.TRUE.equals(entity.isEnabled())) {
            try {
                log.debug("RabbitMQ 消息队列订阅:[{}]开始执行", scriptName);
                RabbitMqDataSourceInfoMagicResourceStorage rabbitMqDataSourceInfoMagicResourceStorage = SpringUtils.getBean(RabbitMqDataSourceInfoMagicResourceStorage.class);
                Map<String, RabbitMqDataSourceInfo> rabbitMqDataSourceInfoMap = ListUtils.emptyIfNull(rabbitMqDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(RabbitMqDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                if(!rabbitMqDataSourceInfoMap.containsKey(entity.getKey())) {
                    Thread.sleep(5000);
                    rabbitMqDataSourceInfoMap = ListUtils.emptyIfNull(rabbitMqDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(RabbitMqDataSourceInfo::getKey, val->val,(k1, k2)->k1));
                    if(!rabbitMqDataSourceInfoMap.containsKey(entity.getKey())) {
                        log.warn("RabbitMQ 订阅:[{}]执行出错, 未找到数据源[{}]", scriptName, entity.getKey());
                        return ;
                    }
                }
                List<RabbitMqSubscribeScriptInfo> newSubscribeList = new ArrayList<>();
                // 添加新的订阅信息
                newSubscribeList.add(new RabbitMqSubscribeScriptInfo(entity, scriptName));
                this.magicDynamicRabbitMqDataSource.putConsumer(rabbitMqDataSourceInfoMap.get(entity.getKey()), newSubscribeList);
            } catch (Exception e) {
                log.error("RabbitMQ 消息队列订阅:[{}]执行出错, {}", scriptName, e);
            }
        }
    }


    /**
     * 注册 RabbitMQ 订阅信息。在注册前会先尝试注销已存在的相同实体。
     *
     * @param entity 要注册的 RabbitMQ 订阅信息实体
     * @return 是否注册成功
     */
    @Override
    public boolean register(RabbitMqSubscribeInfo entity) {
        registerByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据映射节点注册 RabbitMQ 订阅信息，并启动相应的消费者订阅逻辑。
     *
     * @param mappingNode 映射节点，包含要注册的 RabbitMQ 订阅信息实体
     * @return 是否处理成功（始终返回 true）
     */
    @Override
    protected boolean register(AbstractMagicDynamicRegistry.MappingNode<RabbitMqSubscribeInfo> mappingNode) {
        //RabbitMqSubscribeInfo entity = mappingNode.getEntity();
        //RabbitMQConsumer consumer = this.magicDynamicRabbitMqDataSource.getConsumer(entity.getKey());
        //if (Objects.isNull(consumer)) {
        //    registerByEntity(entity);
        //}
        return super.register(mappingNode);
    }

    /**
     * 取消注册指定的 RabbitMQ 订阅信息，并通知对应的消费者取消订阅。
     *
     * @param mappingNode 映射节点，包含要取消注册的 RabbitMQ 订阅信息实体
     */
    @Override
    protected void unregister(AbstractMagicDynamicRegistry.MappingNode<RabbitMqSubscribeInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        super.unregister(mappingNode);
    }


}
