package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model;

import lombok.Data;

@Data
public class WsSubscribeScriptInfo extends WsSubscribeInfo {
    /**
     * 脚本名称
     */
    private String scriptName;


    /**
     * 构造函数，用于创建WsSubscribeScriptInfo对象
     *
     * @param subscribeInfo 订阅信息对象，用于初始化当前对象的订阅信息
     * @param scriptName 脚本名称
     */
    public WsSubscribeScriptInfo(WsSubscribeInfo subscribeInfo, String scriptName) {
        subscribeInfo.copyTo(this);
        this.scriptName = scriptName;
    }
}
