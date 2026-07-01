package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
public class RabbitMqDataSourceInfo extends MagicEntity {
    private String host;
    private String port;
    private String username;
    private String password;
    private String key;
    private String virtualHost;
    private String other;

    public RabbitMqDataSourceInfo() {
    }

    /**
     * 创建一个简单的RabbitMQ数据源信息实体对象
     * 该方法用于创建一个新的RabbitMQ数据源信息对象，并设置基本的连接属性
     *
     * @return MagicEntity 返回配置好的RabbitMQ数据源信息实体对象
     */
    @Override
    public MagicEntity simple() {
        // 创建新的RabbitMQ数据源信息对象
        RabbitMqDataSourceInfo dataSourceInfo = new RabbitMqDataSourceInfo();
        super.simple(dataSourceInfo);
        // 设置RabbitMQ连接的基本属性
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setHost(this.host);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setVirtualHost(this.virtualHost);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    /**
     * 创建一个复制的RabbitMQ数据源信息实体对象
     * 该方法用于创建一个新的RabbitMQ数据源信息对象副本，并设置完整的连接属性
     *
     * @return MagicEntity 返回复制并配置好的RabbitMQ数据源信息实体对象
     */
    @Override
    public MagicEntity copy() {
        // 创建新的RabbitMQ数据源信息对象用于复制
        RabbitMqDataSourceInfo dataSourceInfo = new RabbitMqDataSourceInfo();
        super.copyTo(dataSourceInfo);
        // 复制并设置RabbitMQ的所有连接属性
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setHost(this.host);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setVirtualHost(this.virtualHost);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }


}
