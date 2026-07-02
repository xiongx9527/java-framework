package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

import java.util.Objects;

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
    public KafkaSubscribeInfo copy() {
        KafkaSubscribeInfo info = new KafkaSubscribeInfo();
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

    /**
     * 比较当前对象与另一个对象是否相等
     *
     * @param o 要比较的对象
     * @return 如果两个对象相等返回true，否则返回false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        KafkaSubscribeInfo kafkaSubscribeInfo = (KafkaSubscribeInfo) o;
        return Objects.equals(id, kafkaSubscribeInfo.id) &&
                Objects.equals(path, kafkaSubscribeInfo.path) &&
                Objects.equals(script, kafkaSubscribeInfo.script) &&
                Objects.equals(name, kafkaSubscribeInfo.name) &&
                Objects.equals(key, kafkaSubscribeInfo.key) &&
                Objects.equals(topic, kafkaSubscribeInfo.topic) &&
                Objects.equals(partition, kafkaSubscribeInfo.partition) &&
                Objects.equals(other, kafkaSubscribeInfo.other) &&
                Objects.equals(enabled, kafkaSubscribeInfo.enabled);
    }

    /**
     * 计算当前对象的哈希码
     *
     * @return 返回基于对象关键属性计算出的哈希码值
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, path, script, name, groupId, topic, partition, enabled, other);
    }

}
