package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

@Data
public class RabbitMqSubscribeInfo extends PathMagicEntity {

    /**
     * 数据源Key
     */
    private String key;

    /**
     * 队列名称
     */
    private String queueName;

    /**
     * 广播
     */
    private boolean broadcast =  false;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 其它配置
     */
    private String other;

    /**
     * 参数
     */
    private String params;


    /**
     * 默认构造函数
     */
    public RabbitMqSubscribeInfo() {
    }

    /**
     * 创建当前对象的副本
     * @return 返回一个新的RabbitMqSubscribeInfo对象，包含与当前对象相同的数据
     */
    public RabbitMqSubscribeInfo copy() {
        RabbitMqSubscribeInfo info = new RabbitMqSubscribeInfo();
        super.copyTo(info);
        info.setKey(this.key);
        info.setQueueName(this.queueName);
        info.setEnabled(this.enabled);
        info.setOther(this.other);
        info.setParams(this.params);
        info.setBroadcast(this.broadcast);
        return info;
    }

    /**
     * 将当前对象的数据复制到指定的目标对象中
     * @param info 目标RabbitMqSubscribeInfo对象
     * @return 返回填充了数据的目标对象
     */
    public RabbitMqSubscribeInfo copyTo(RabbitMqSubscribeInfo info) {
        super.copyTo(info);
        info.setKey(this.key);
        info.setQueueName(this.queueName);
        info.setEnabled(this.enabled);
        info.setOther(this.other);
        info.setParams(this.params);
        info.setBroadcast(this.broadcast);
        return info;
    }

    /**
     * 创建当前对象的简化版本
     * @return 返回一个新的MagicEntity对象，包含当前对象的基本信息
     */
    public MagicEntity simple() {
        RabbitMqSubscribeInfo info = new RabbitMqSubscribeInfo();
        super.simple(info);
        return info;
    }

}
