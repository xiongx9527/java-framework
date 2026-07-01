package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model;


import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

/**
 * 订阅信息
 *
 * @author zzy
 */
@Data
public class RocketMqSubscribeInfo extends PathMagicEntity {

    /**
     * 数据源Key
     */
    private String key;

    /**
     * 主题
     */
    private String topic;

    /**
     * 广播
     */
    private boolean broadcast =  false;

    /**
     * 订阅组
     */
    private String group = "magic-api";

    /**
     * 标签字符串，用于存储标签信息
     */
    private String tags = "*";

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

    public RocketMqSubscribeInfo copy() {
        RocketMqSubscribeInfo info = new RocketMqSubscribeInfo();
        super.copyTo(info);
        info.setKey(this.key);
        info.setTopic(this.topic);
        info.setEnabled(this.enabled);
        info.setOther(this.other);
        info.setParams(this.params);
        info.setBroadcast(this.broadcast);
        info.setGroup(this.group);
        info.setTags(this.tags);
        return info;
    }

    public RocketMqSubscribeInfo copyTo(RocketMqSubscribeInfo info) {
        super.copyTo(info);
        info.setKey(this.key);
        info.setTopic(this.topic);
        info.setEnabled(this.enabled);
        info.setOther(this.other);
        info.setParams(this.params);
        info.setBroadcast(this.broadcast);
        info.setGroup(this.group);
        info.setTags(this.tags);
        return info;
    }

    @Override
    public MagicEntity simple() {
        RocketMqSubscribeInfo info = new RocketMqSubscribeInfo();
        super.simple(info);
        return info;
    }
}
