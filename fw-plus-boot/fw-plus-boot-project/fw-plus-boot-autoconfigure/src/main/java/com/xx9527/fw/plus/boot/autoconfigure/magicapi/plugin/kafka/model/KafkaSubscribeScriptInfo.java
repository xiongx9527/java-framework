package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model;

import lombok.Data;

@Data
public class KafkaSubscribeScriptInfo extends KafkaSubscribeInfo {
    /**
     * 脚本名称
     */
    private String scriptName;

    /**
     * 构造函数，创建Kafka订阅脚本信息实例
     * 
     * @param subscribeInfo Kafka订阅信息对象，用于复制基础订阅信息
     * @param scriptName 脚本名称
     */
    public KafkaSubscribeScriptInfo(KafkaSubscribeInfo subscribeInfo, String scriptName) {
        subscribeInfo.copyTo(this);
        this.scriptName = scriptName;
    }
}
