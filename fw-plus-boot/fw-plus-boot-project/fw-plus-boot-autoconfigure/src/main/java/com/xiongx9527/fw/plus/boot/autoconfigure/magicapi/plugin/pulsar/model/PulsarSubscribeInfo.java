package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model;

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
public class PulsarSubscribeInfo extends PathMagicEntity {

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
     * 批量索引确认功能是否启用的标志位
     *
     * 该字段用于控制批量索引操作的确认机制是否开启。
     * 当设置为true时，系统会在批量索引操作完成后发送确认信息；
     * 当设置为false时，系统将跳过确认步骤以提高性能。
     */
    private boolean batchIndexAcknowledgmentEnabled = true;

    /**
     * 其它配置
     */
    private String other;

    /**
     * 参数
     */
    private String params;


    /**
     * 创建当前对象的一个副本。
     * <p>
     * 此方法会创建一个新的 {@link PulsarSubscribeInfo} 实例，并将当前对象的所有属性复制到新实例中，
     * 其中 path 属性被显式设置为 null。
     *
     * @return 返回一个新的 {@link PulsarSubscribeInfo} 对象，其属性与当前对象一致（path 除外）
     */
    public PulsarSubscribeInfo copy() {
        PulsarSubscribeInfo info = new PulsarSubscribeInfo();
        super.copyTo(info);
        info.setKey(this.key);
        info.setTopic(this.topic);
        info.setGroup(this.group);
        info.setPartition(this.partition);
        info.setEnabled(this.enabled);
        info.setOther(this.other);
        info.setParams(this.params);
        info.setBatchIndexAcknowledgmentEnabled(this.batchIndexAcknowledgmentEnabled);
        info.setPath(null);
        return info;
    }

    /**
     * 将当前对象的属性复制到指定的目标对象中。
     * <p>
     * 该方法首先调用父类的 copyTo 方法进行基础属性复制，然后依次设置 key、topic、group、partition、enabled、other 和 params 属性。
     * 最后将目标对象的 path 设置为 null。
     *
     * @param info 目标 {@link PulsarSubscribeInfo} 对象，用于接收当前对象的属性值
     * @return 复制完成后的目标对象
     */
    public PulsarSubscribeInfo copyTo(PulsarSubscribeInfo info) {
        super.copyTo(info);
        info.setKey(this.key);
        info.setTopic(this.topic);
        info.setGroup(this.group);
        info.setPartition(this.partition);
        info.setEnabled(this.enabled);
        info.setOther(this.other);
        info.setParams(this.params);
        info.setBatchIndexAcknowledgmentEnabled(this.batchIndexAcknowledgmentEnabled);
        info.setPath(null);
        return info;
    }

    /**
     * 创建一个简化版本的对象。
     * <p>
     * 本方法通过调用父类的 simple 方法来构造一个仅包含基本字段的新对象。
     *
     * @return 简化后的 {@link MagicEntity} 对象
     */
    @Override
    public MagicEntity simple() {
        PulsarSubscribeInfo info = new PulsarSubscribeInfo();
        super.simple(info);
        return info;
    }

    /**
     * 判断两个对象是否相等。
     * <p>
     * 首先判断是否是同一个引用或类型不同，如果是则直接返回 false；接着比较所有关键属性是否相同。
     *
     * @param o 要比较的对象
     * @return 如果两个对象在逻辑上相等则返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PulsarSubscribeInfo pulsarSubscribeInfo = (PulsarSubscribeInfo) o;
        // 比较各个关键字段是否一致
        return Objects.equals(id, pulsarSubscribeInfo.id) &&
                Objects.equals(path, pulsarSubscribeInfo.path) &&
                Objects.equals(script, pulsarSubscribeInfo.script) &&
                Objects.equals(name, pulsarSubscribeInfo.name) &&
                Objects.equals(key, pulsarSubscribeInfo.key) &&
                Objects.equals(topic, pulsarSubscribeInfo.topic) &&
                Objects.equals(group, pulsarSubscribeInfo.group) &&
                Objects.equals(partition, pulsarSubscribeInfo.partition) &&
                Objects.equals(other, pulsarSubscribeInfo.other) &&
                Objects.equals(enabled, pulsarSubscribeInfo.enabled);
    }

    /**
     * 计算当前对象的哈希码。
     * <p>
     * 基于 id、path、script、name、groupId、topic、group、partition、enabled 和 other 字段计算得出。
     *
     * @return 当前对象的哈希码
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, path, script, name, groupId, topic,group, partition, enabled, other);
    }

}
