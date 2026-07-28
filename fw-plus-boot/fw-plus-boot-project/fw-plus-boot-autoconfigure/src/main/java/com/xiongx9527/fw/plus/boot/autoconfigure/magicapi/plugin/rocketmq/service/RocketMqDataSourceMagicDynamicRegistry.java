package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.MagicDynamicRocketMqDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;
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
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听分组事件，当事件类型为 'rocketmq-datasource' 时处理
     */
    @EventListener(condition = "#event.type == 'rocketmq-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
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
        unregisterByEntity(info);
        log.info("注册 RocketMQ 消息队列:[{}]开始执行", info.getKey());
        boolean registerProducer = this.magicDynamicRocketMqDataSource.putProducer(info);
        if (!registerProducer) {
            log.error("RocketMQ 消息队列:[{}]注册生产者失败", info.getKey());
        } else {
            log.info("RocketMQ 消息队列:[{}]注册生产者成功", info.getKey());
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
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<RocketMqDataSourceInfo> mappingNode) {
        registerByEntity(mappingNode.getEntity());
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<RocketMqDataSourceInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<RocketMqDataSourceInfo> defaultMappings() {
        List<RocketMqDataSourceInfo> dataSourceInfoList = this.magicDynamicRocketMqDataSource.producerNodes().stream().filter(it -> it.getId() == null).map(it -> {
            RocketMqDataSourceInfo dataSourceInfo = new RocketMqDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());

        try {
            DefaultMQProducer producer = SpringUtils.getBean(DefaultMQProducer.class);
            if (producer != null) {
                RocketMqDataSourceInfo dataSourceInfo = new RocketMqDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
            log.debug("获取默认 DefaultMQProducer 失败", e);
        }

        return dataSourceInfoList;
    }


}
