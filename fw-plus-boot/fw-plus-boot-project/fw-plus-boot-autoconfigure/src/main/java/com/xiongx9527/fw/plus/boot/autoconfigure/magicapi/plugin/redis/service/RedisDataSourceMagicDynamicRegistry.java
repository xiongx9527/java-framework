package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.service;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RedisDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<RedisDataSourceInfo> {

    /**
     * Redis数据源管理器
     * 用于动态创建、销毁和管理Redis连接工厂实例
     */
    private final MagicDynamicRedisDataSource magicRedisDataSource;

    /**
     * 构造函数
     * 初始化Redis数据源动态注册器
     *
     * @param magicResourceStorage Redis数据源信息存储器
     * @param magicRedisDataSource Redis数据源管理器
     */
    public RedisDataSourceMagicDynamicRegistry(MagicResourceStorage<RedisDataSourceInfo> magicResourceStorage, MagicDynamicRedisDataSource magicRedisDataSource) {
        super(magicResourceStorage);
        this.magicRedisDataSource = magicRedisDataSource;
    }

    /**
     * 处理文件事件
     * 当接收到类型为"redis-datasource"的文件事件时，调用processEvent方法进行处理
     *
     * @param event 文件事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'redis-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 Redis 数据源文件事件失败: {}", event, e);
        }
    }

    /**
     * 处理分组事件
     * 当接收到类型为"redis-datasource"的分组事件时，调用processEvent方法进行处理
     *
     * @param event 分组事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'redis-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 Redis 数据源分组事件失败: {}", event, e);
        }
    }


    @Override
    public boolean register(RedisDataSourceInfo entity) {
        magicRedisDataSource.destroyRedisConnectionFactoryByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception e) {
            log.warn("注册 Redis 数据源失败: {}", entity == null ? null : entity.getKey(), e);
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<RedisDataSourceInfo> mappingNode) {
        magicRedisDataSource.destroyRedisConnectionFactoryByKey(mappingNode.getEntity().getKey());
        magicRedisDataSource.buildRedisConnectionFactory(mappingNode.getEntity());
        try {
            return super.register(mappingNode);
        } catch (Exception e) {
            log.warn("注册 Redis 数据源映射失败: {}", mappingNode.getEntity() == null ? null : mappingNode.getEntity().getKey(), e);
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<RedisDataSourceInfo> mappingNode) {
        magicRedisDataSource.destroyRedisConnectionFactoryByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception e) {
            log.warn("注销 Redis 数据源失败: {}", mappingNode.getEntity().getKey(), e);
        }
    }

    /**
     * 获取默认的Redis数据源映射列表
     * 过滤出没有ID的数据源节点，并将其转换为RedisDataSourceInfo对象列表
     *
     * @return List<RedisDataSourceInfo> 默认的Redis数据源信息列表
     */
    @Override
    public List<RedisDataSourceInfo> defaultMappings() {
        return magicRedisDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            RedisDataSourceInfo dataSourceInfo = new RedisDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), I18nMessageResolver.resolveKey("fw.boot.datasource.defaultName"))));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }
}
