package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

/**
 * 订阅信息
 *
 * @author zzy
 */
@Data
public class KafkaSubscribeInfo extends PathMagicEntity {

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
     * 分区
     */
    private Integer partition;

    /**
     * 是否启用
     */
    private boolean enabled;

     /**
     * 确认模式
     */
    private String ackMode = "BATCH";

    /**
     * 确认计数器，用于记录确认消息的数量
     */
    private Integer ackCount = 100;

    /**
     * 确认时间戳，用于记录最后一次确认的时间
     */
    private Long ackTime = 5000L;

    /**
     * 并发数
     */
    private Integer concurrency;

    /**
     * 其它配置
     */
    private String other;

    /**
     * 参数
     */
    private String params;

    /**
     * 创建当前对象的副本
     *
     * @return 返回一个新的KafkaSubscribeInfo对象，包含与当前对象相同的数据
     */
    @Override
    public KafkaSubscribeInfo copy() {
        KafkaSubscribeInfo info = new KafkaSubscribeInfo();
        copyTo(info);
        return info;
    }

    /**
     * 将当前对象的数据复制到指定的目标对象中
     *
     * @param info 目标KafkaSubscribeInfo对象，用于接收当前对象的数据
     * @return 返回填充了数据的目标KafkaSubscribeInfo对象
     */
    public KafkaSubscribeInfo copyTo(KafkaSubscribeInfo info) {
        super.copyTo(info);
        info.setKey(this.key);
        info.setTopic(this.topic);
        info.setPartition(this.partition);
        info.setEnabled(this.enabled);
        info.setOther(this.other);
        info.setParams(this.params);
        info.setGroup(this.group);
        info.setAckMode(this.ackMode);
        info.setAckCount(this.ackCount);
        info.setAckTime(this.ackTime);
        info.setPath(null);
        info.setConcurrency(this.concurrency);
        info.setBroadcast(this.broadcast);
        return info;
    }

    /**
     * 创建当前对象的简化版本
     *
     * @return 返回一个新的MagicEntity对象，包含当前对象的核心信息
     */
    @Override
    public MagicEntity simple() {
        KafkaSubscribeInfo info = new KafkaSubscribeInfo();
        super.simple(info);
        return info;
    }

}
