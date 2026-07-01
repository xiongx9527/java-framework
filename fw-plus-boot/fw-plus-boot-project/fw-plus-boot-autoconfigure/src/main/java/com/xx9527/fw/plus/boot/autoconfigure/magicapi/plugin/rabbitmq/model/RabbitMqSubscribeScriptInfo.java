package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model;

import lombok.Data;


/**
 * RabbitMQ订阅脚本信息类
 * <p>
 * 该类继承自RabbitMqSubscribeInfo，用于封装RabbitMQ订阅相关的脚本信息
 * </p>
 */
@Data
public class RabbitMqSubscribeScriptInfo extends RabbitMqSubscribeInfo {
    /**
     * 脚本名称变量
     * 用于存储脚本的名称信息
     */
    private String scriptName;

    /**
     * 构造函数
     * <p>
     * 使用给定的订阅信息和脚本名称创建RabbitMqSubscribeScriptInfo实例
     * </p>
     *
     * @param subscribeInfo 订阅信息对象，用于初始化当前对象的订阅相关属性
     * @param scriptName 脚本名称
     */
    public RabbitMqSubscribeScriptInfo(RabbitMqSubscribeInfo subscribeInfo, String scriptName) {
        subscribeInfo.copyTo(this);
        this.scriptName = scriptName;
    }
}

