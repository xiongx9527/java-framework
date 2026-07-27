package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class RedisDataSourceInfo extends MagicEntity {
    /**
     * 数据源key
     */
    private String key;

    /**
     * 数据库主机名
     */
    private String hostname;

    /**
     * 数据库端口
     */
    private String port;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码
     */
    private String password;

    /**
     * 数据库
     */
    private String database;

    /**
     * 客户端类型
     */
    private String clientType = "lettuce";

    /**
     * 集群模式
     */
    private String mode;

    /**
     * 其它配置
     */
    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    @Override
    public MagicEntity simple() {
        RedisDataSourceInfo dataSourceInfo = new RedisDataSourceInfo();
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHostname(this.hostname);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setDatabase(this.database);
        dataSourceInfo.setClientType(clientType);
        dataSourceInfo.setMode(mode);
        dataSourceInfo.setProperties(copyProperties());
        return dataSourceInfo;
    }

    @Override
    public MagicEntity copy() {
        RedisDataSourceInfo dataSourceInfo = new RedisDataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHostname(this.hostname);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setDatabase(this.database);
        dataSourceInfo.setClientType(clientType);
        dataSourceInfo.setMode(mode);
        dataSourceInfo.setProperties(copyProperties());
        return dataSourceInfo;
    }

    private Map<String, Object> copyProperties() {
        return this.properties == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(this.properties);
    }
}
