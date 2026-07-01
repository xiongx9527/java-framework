package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.MagicDynamicRabbitMqDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RabbitMqDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<RabbitMqDataSourceInfo> {
    /**
     * 魔法动态RabbitMQ数据源实例
     */
    private final MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource;

    /**
     * 构造函数，初始化RabbitMq数据源魔法动态注册器
     *
     * @param magicResourceStorage 魔法资源存储器，用于存储RabbitMq数据源信息
     * @param magicDynamicRabbitMqDataSource 魔法动态RabbitMQ数据源实例
     */
    public RabbitMqDataSourceMagicDynamicRegistry(MagicResourceStorage<RabbitMqDataSourceInfo> magicResourceStorage, MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource) {
        super(magicResourceStorage);
        this.magicDynamicRabbitMqDataSource = magicDynamicRabbitMqDataSource;
    }

    /**
     * 获取魔法动态RabbitMQ数据源实例
     *
     * @return 返回魔法动态RabbitMQ数据源实例
     */
    public MagicDynamicRabbitMqDataSource getMagicDynamicRabbitMqDataSource() {
        return this.magicDynamicRabbitMqDataSource;
    }

    /**
     * 监听文件事件，当事件类型为'rabbitmq-datasource'时触发处理
     * @param event 文件事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'rabbitmq-datasource'")
    public void onFileEvent(FileEvent event) {
        this.processEvent(event);
    }

    /**
     * 监听组事件，当事件类型为'rabbitmq-datasource'时触发处理
     * @param event 组事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'rabbitmq-datasource'")
    public void onGroupEvent(GroupEvent event) {
        this.processEvent(event);
    }

    /**
     * 根据实体信息注销RabbitMQ数据源的生产者和消费者
     *
     * @param info RabbitMQ数据源信息对象，包含要注销的生产者和消费者的标识信息
     */
    private void unregisterByEntity(RabbitMqDataSourceInfo info) {
        // 删除指定key的生产者
        this.magicDynamicRabbitMqDataSource.deleteProducer(info.getKey());
    }


    /**
     * 根据数据源信息注册RabbitMQ生产者和消费者
     *
     * @param info RabbitMQ数据源信息，包含连接配置、队列信息等关键参数
     */
    private void registerByEntity(RabbitMqDataSourceInfo info) {
        // 先注销已存在的数据源配置
        this.unregisterByEntity(info);
        log.info("注册 Rabbit 消息队列:[{}]开始执行", info.getKey());

        // 注册生产者
        boolean registerProducer = this.magicDynamicRabbitMqDataSource.putProducer(info);
        if (!registerProducer) {
            log.error("Rabbit 消息队列:[{}]注册生产者失败", info.getKey());
        } else {
            log.info("Rabbit 消息队列:[{}]注册生产者成功", info.getKey());
        }
    }

    /**
     * 注册RabbitMQ数据源信息
     * 先取消注册相同实体，再进行注册操作
     * @param entity RabbitMQ数据源信息实体
     * @return 注册是否成功
     */
    @Override
    public boolean register(RabbitMqDataSourceInfo entity) {
        unregisterByEntity(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<RabbitMqDataSourceInfo> mappingNode) {
        registerByEntity(mappingNode.getEntity());
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 取消注册RabbitMQ数据源映射节点
     * 从动态数据源中删除指定key的生产者
     * @param mappingNode RabbitMQ数据源映射节点，用于获取要删除的实体信息
     */
    @Override
    protected void unregister(AbstractMagicDynamicRegistry.MappingNode<RabbitMqDataSourceInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取默认的数据源映射配置列表
     *
     * <p>该方法首先从生产者节点中查找没有设置ID的节点，将其转换为数据源信息；
     * 如果生产者节点中没有找到符合条件的节点，则从消费者节点中查找没有设置ID的节点进行转换。</p>
     *
     * @return 包含默认数据源信息的列表，每个数据源信息包含名称和键值
     */
    @Override
    public List<RabbitMqDataSourceInfo> defaultMappings() {
        // 从生产者节点中获取没有ID的节点，并转换为数据源信息列表
        List<RabbitMqDataSourceInfo> dataSourceInfoList = this.magicDynamicRabbitMqDataSource.producerNodes().stream().filter(it -> it.getId() == null).map(it -> {
            RabbitMqDataSourceInfo dataSourceInfo = new RabbitMqDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
        try {
            RabbitTemplate rabbitTemplate = SpringUtils.getBean(RabbitTemplate.class);
            if (rabbitTemplate != null
                    && rabbitTemplate.getConnectionFactory() != null) {
                CachingConnectionFactory connectionFactory = (CachingConnectionFactory) rabbitTemplate.getConnectionFactory();
                String host =connectionFactory.getHost();
                String username = connectionFactory.getUsername();
                int port = connectionFactory.getPort();
                if (StringUtils.isNotBlank(host)
                        && StringUtils.isNotBlank(username)
                        && (!("localhost".equals(host) || "127.0.0.1".equals(host))
                        && !"guest".equals(username))
                      && port != -1) {
                    RabbitMqDataSourceInfo dataSourceInfo = new RabbitMqDataSourceInfo();
                    dataSourceInfo.setKey("default");
                    dataSourceInfo.setName("默认数据源");
                    dataSourceInfoList.add(dataSourceInfo);
                }
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

}
