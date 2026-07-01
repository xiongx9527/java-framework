package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class MqttDataSourceInfo extends MagicEntity {

    /**
     * 数据源key
     */
    private String key;


    /**
     * IP地址配置项
     * 用于指定服务监听的IP地址
     */
    private String ip;

    /**
     * 端口号配置项
     * 用于指定服务监听的端口号
     */
    private String port = "1883";

    /**
     * Web端口配置项
     * 用于指定Web服务监听的端口号
     */
    private String webPort = "8083";

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /**
     * 其它配置
     */
    private String other;

    @Override
    public MagicEntity simple() {
        // 创建Weaviate数据源信息对象
        MqttDataSourceInfo dataSourceInfo = new MqttDataSourceInfo();
        // 调用父类的simple方法进行基础配置
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setIp(this.ip);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setWebPort(this.webPort);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    /**
     * 创建当前数据源信息对象的深拷贝副本
     *
     * @return 返回一个新的WeaviateDataSourceInfo对象，包含与当前对象相同的所有属性值
     */
    @Override
    public MagicEntity copy() {
        MqttDataSourceInfo dataSourceInfo = new MqttDataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setIp(this.ip);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setWebPort(this.webPort);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

}
