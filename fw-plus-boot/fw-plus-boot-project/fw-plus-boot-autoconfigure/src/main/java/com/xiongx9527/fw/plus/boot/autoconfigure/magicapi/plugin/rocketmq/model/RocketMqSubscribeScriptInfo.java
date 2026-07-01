package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model;

import lombok.Data;

@Data
public class RocketMqSubscribeScriptInfo extends RocketMqSubscribeInfo {
    /**
     * 脚本名称
     */
    private String scriptName;

    public RocketMqSubscribeScriptInfo(RocketMqSubscribeInfo subscribeInfo, String scriptName) {
        subscribeInfo.copyTo(this);
        this.scriptName = scriptName;
    }
}
