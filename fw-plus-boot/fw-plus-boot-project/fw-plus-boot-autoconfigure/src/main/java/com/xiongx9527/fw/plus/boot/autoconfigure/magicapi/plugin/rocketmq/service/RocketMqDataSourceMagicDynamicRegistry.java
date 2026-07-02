package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.RocketMQConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.MagicDynamicRocketMqDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeScriptInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.producer.RocketMqProducer;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.MQProducer;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RocketMq 数据源 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class RocketMqDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<RocketMqDataSourceInfo> {

    /**
     * MagicDynamicRocketMqDataSource实例，用于动态管理RocketMQ数据源
     *
     * 该字段为final类型，确保在对象初始化后不可更改，
     * 提供了对RocketMQ数据源的统一访问入口
     */
    private final MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource;

    /**
     * RocketMqDataSourceMagicDynamicRegistry 构造函数
     * @param magicResourceStorage 资源存储器，用于存储 RocketMqDataSourceInfo 对象
     * @param magicDynamicRocketMqDataSource 动态 RocketMQ 数据源对象，用于管理生产者和消费者
     */
    public RocketMqDataSourceMagicDynamicRegistry(MagicResourceStorage<RocketMqDataSourceInfo> magicResourceStorage,
                                                  MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource) {
        super(magicResourceStorage);
        this.magicDynamicRocketMqDataSource = magicDynamicRocketMqDataSource;
    }

    /**
     * 获取动态 RocketMQ 数据源对象
     * @return 返回 MagicDynamicRocketMqDataSource 对象
     */
    public MagicDynamicRocketMqDataSource getMagicDynamicRocketMqDataSource() {
        return magicDynamicRocketMqDataSource;
    }

    /**
     * 监听文件事件，当事件类型为 'rocketmq-datasource' 时处理该事件
     * @param event 文件事件对象
     */
    @EventListener(condition = "#event.type == 'rocketmq-datasource'")
    public void onFileEvent(FileEvent event) {
        processEvent(event);
    }

    /**
     * 监听分组事件，当事件类型为 'rocketmq-datasource' 时处理该事件
     * @param event 分组事件对象
     */
    @EventListener(condition = "#event.type == 'rocketmq-datasource'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }

    /**
     * 根据实体信息注销 RocketMQ 数据源
     * @param info RocketMQ 数据源信息对象
     */
    private void unregisterByEntity(RocketMqDataSourceInfo info) {
        // 删除指定key的生产者
        this.magicDynamicRocketMqDataSource.deleteProducer(info.getKey());
    }

    /**
     * 根据实体信息注册 RocketMQ 数据源
     * @param info RocketMQ 数据源信息对象
     */
    private void registerByEntity(RocketMqDataSourceInfo info) {
        // 先注销已存在的数据源配置
        this.unregisterByEntity(info);
        log.info("注册 rocketmq 消息队列:[{}]开始执行", info.getKey());
        // 注册生产者
        boolean registerProducer = this.magicDynamicRocketMqDataSource.putProducer(info);
        if (!registerProducer) {
            log.error("rocketmq 消息队列:[{}]注册生产者失败", info.getKey());
        } else {
            log.info("rocketmq 消息队列:[{}]注册生产者成功", info.getKey());
        }
    }

    /**
     * 注册指定的 RocketMQ 数据源信息。
     * 在注册之前会先尝试注销已存在的相同实体，以避免重复注册。
     *
     * @param entity 要注册的 RocketMQ 数据源信息对象
     * @return 是否注册成功
     */
    @Override
    public boolean register(RocketMqDataSourceInfo entity) {
        unregisterByEntity(entity);
        return super.register(entity);
    }

    /**
     * 注册指定的映射节点中的 RocketMQ 数据源信息。
     * 该方法会记录日志并在注册前后输出相关信息。
     *
     * @param mappingNode 包含要注册的数据源信息的映射节点
     * @return 始终返回 false（根据当前实现）
     */
    @Override
    protected boolean register(MappingNode<RocketMqDataSourceInfo> mappingNode) {
        RocketMqDataSourceInfo entity = mappingNode.getEntity();
        log.info("注册 Rabbit 消息队列:[{}]开始执行", entity.getKey());
        registerByEntity(entity);
        log.info("注册 Rabbit 消息队列：[{}]完成执行", entity.getKey());
        return false;
    }

    /**
     * 取消注册指定映射节点中的 RocketMQ 数据源信息。
     * 执行前记录开始日志，完成后记录结束日志，并调用父类的注销逻辑。
     *
     * @param mappingNode 包含要注销的数据源信息的映射节点
     */
    @Override
    protected void unregister(MappingNode<RocketMqDataSourceInfo> mappingNode) {
        log.info("取消注册 Rabbit 消息队列:[{}]开始执行", mappingNode.getEntity().getKey());
        unregisterByEntity(mappingNode.getEntity());
        log.info("取消注册 Rabbit 消息队列：[{}]完成执行", mappingNode.getEntity().getKey());
        super.unregister(mappingNode);
    }

    /**
     * 获取默认的 RocketMQ 数据源信息列表。
     * 列表包括没有设置 ID 的生产者节点以及 Spring 容器中存在的 MQProducer 实例对应的默认数据源。
     *
     * @return 默认的 RocketMQ 数据源信息列表
     */
    @Override
    public List<RocketMqDataSourceInfo> defaultMappings() {
        // 从生产者节点中获取没有ID的节点，并转换为数据源信息列表
        List<RocketMqDataSourceInfo> dataSourceInfoList = this.magicDynamicRocketMqDataSource.producerNodes().stream().filter(it -> it.getId() == null).map(it -> {
            RocketMqDataSourceInfo dataSourceInfo = new RocketMqDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());

        // 尝试从 Spring 上下文中获取 MQProducer 并添加默认数据源
        try {
            RocketMqProducer mqProducer = SpringUtils.getBean(RocketMqProducer.class);
            if (mqProducer != null) {
                RocketMqDataSourceInfo dataSourceInfo = new RocketMqDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName(Options.DEFAULT_DATA_SOURCE.getName());
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
            // 忽略异常，不影响主流程
        }

        return dataSourceInfoList;
    }


}
