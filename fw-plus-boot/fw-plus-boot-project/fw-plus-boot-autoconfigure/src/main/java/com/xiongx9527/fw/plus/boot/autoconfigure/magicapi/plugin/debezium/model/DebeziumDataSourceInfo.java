package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DebeziumDataSourceInfo extends MagicEntity {
    /**
     * 数据源key
     */
    private String key;

    /**
     * 连接器类
     */
    private String connectorClass;

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
     * 数据库名
     */
    private String database;

    /**
     * 包含的表列表
     */
    private String includeTables;

    /**
     * 排除的表列表
     */
    private String excludeTables;

    /**
     * Kafka服务器地址
     */
    private String kafkaServers;

    /**
     * Kafka topic名称
     */
    private String kafkaTopic;

    /**
     * 其他配置项
     */
    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    /**
     * 成员变量，表示当前对象的启用状态
     *
     * 该变量用于控制对象的功能是否可用，默认初始化为false（禁用状态）
     */
    private boolean enabled = false;

    /**
     * server.id 生成策略：random（随机）或 fixed（固定），默认 random
     */
    private String serverIdType = "random";


    /**
     * 创建一个简单的数据源信息对象
     * 该方法用于创建并初始化Debezium数据源的基本配置信息
     *
     * @return MagicEntity 返回初始化后的Debezium数据源信息对象
     */
    @Override
    public MagicEntity simple() {
        // 创建新的Debezium数据源信息实例
        DebeziumDataSourceInfo dataSourceInfo = new DebeziumDataSourceInfo();
        // 调用父类的simple方法进行基础初始化
        super.simple(dataSourceInfo);
        // 设置Debezium连接器的核心配置参数
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setConnectorClass(this.connectorClass);
        dataSourceInfo.setHostname(this.hostname);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setDatabase(this.database);
        dataSourceInfo.setIncludeTables(this.includeTables);
        dataSourceInfo.setExcludeTables(this.excludeTables);
        dataSourceInfo.setKafkaServers(this.kafkaServers);
        dataSourceInfo.setKafkaTopic(this.kafkaTopic);
        dataSourceInfo.setProperties(copyProperties());
        dataSourceInfo.setEnabled(this.enabled);
        dataSourceInfo.setServerIdType(this.serverIdType);
        return dataSourceInfo;
    }

    /**
     * 创建一个完整的数据源信息副本
     * 该方法用于复制当前数据源的所有配置信息，包括敏感信息如密码
     *
     * @return MagicEntity 返回包含完整配置信息的Debezium数据源副本
     */
    @Override
    public MagicEntity copy() {
        // 创建新的Debezium数据源信息实例用于复制
        DebeziumDataSourceInfo dataSourceInfo = new DebeziumDataSourceInfo();
        // 调用父类的copyTo方法进行基础属性复制
        super.copyTo(dataSourceInfo);
        // 复制所有Debezium连接器的配置参数，包括密码等敏感信息
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setConnectorClass(this.connectorClass);
        dataSourceInfo.setHostname(this.hostname);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setDatabase(this.database);
        dataSourceInfo.setIncludeTables(this.includeTables);
        dataSourceInfo.setExcludeTables(this.excludeTables);
        dataSourceInfo.setKafkaServers(this.kafkaServers);
        dataSourceInfo.setKafkaTopic(this.kafkaTopic);
        dataSourceInfo.setProperties(copyProperties());
        dataSourceInfo.setEnabled(this.enabled);
        dataSourceInfo.setServerIdType(this.serverIdType);
        return dataSourceInfo;
    }

    private Map<String, Object> copyProperties() {
        return this.properties == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(this.properties);
    }

}
