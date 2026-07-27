package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class MongoDataSourceInfo extends MagicEntity {
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
     * 集群模式
     */
    private String mode;

    /**
     * 其它配置
     */
    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    /**
     * 创建数据源信息的简单副本
     * <p>
     * 该方法创建一个新的MongoDataSourceInfo实例，并将当前对象的基本属性复制到新实例中。
     * 首先调用父类的simple方法进行基础初始化，然后设置MongoDB特定的连接属性。
     * </p>
     *
     * @return MagicEntity 返回配置好的数据源信息对象
     */
    @Override
    public MagicEntity simple() {
        // 创建新的Mongo数据源信息对象
        MongoDataSourceInfo dataSourceInfo = new MongoDataSourceInfo();
        // 调用父类方法进行基础初始化
        super.simple(dataSourceInfo);
        // 设置MongoDB连接所需的基本属性
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHostname(this.hostname);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setProperties(copyProperties());
        return dataSourceInfo;
    }

    /**
     * 创建数据源信息的完整副本
     * <p>
     * 该方法创建一个新的MongoDataSourceInfo实例，并将当前对象的所有属性复制到新实例中。
     * 与simple方法不同，这里调用父类的copyTo方法来确保所有继承属性都被正确复制。
     * </p>
     *
     * @return MagicEntity 返回配置好的数据源信息对象副本
     */
    @Override
    public MagicEntity copy() {
        // 创建新的Mongo数据源信息对象用于复制
        MongoDataSourceInfo dataSourceInfo = new MongoDataSourceInfo();
        // 调用父类方法将当前对象属性复制到新对象
        super.copyTo(dataSourceInfo);
        // 设置MongoDB连接所需的基本属性
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHostname(this.hostname);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setProperties(copyProperties());
        return dataSourceInfo;
    }

    private Map<String, Object> copyProperties() {
        return this.properties == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(this.properties);
    }

}
