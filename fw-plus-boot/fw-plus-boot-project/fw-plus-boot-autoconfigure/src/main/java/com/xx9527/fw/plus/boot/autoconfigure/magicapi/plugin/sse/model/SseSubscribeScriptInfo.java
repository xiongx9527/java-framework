package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model;

import lombok.Data;

@Data
public class SseSubscribeScriptInfo extends SseSubscribeInfo {
    /**
     * 脚本名称
     */
    private String scriptName;

    /**
     * 构造函数，用于创建SseSubscribeScriptInfo对象
     * @param subscribeInfo 订阅信息对象，用于复制基础订阅信息
     * @param scriptName 脚本名称
     */
    public SseSubscribeScriptInfo(SseSubscribeInfo subscribeInfo, String scriptName) {
        subscribeInfo.copyTo(this);
        this.scriptName = scriptName;
    }

}
