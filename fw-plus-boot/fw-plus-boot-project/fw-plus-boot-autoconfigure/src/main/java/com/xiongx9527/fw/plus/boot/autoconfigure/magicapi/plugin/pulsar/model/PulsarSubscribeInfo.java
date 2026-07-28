package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

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
    @Override
    public PulsarSubscribeInfo copy() {
        PulsarSubscribeInfo info = new PulsarSubscribeInfo();
        copyTo(info);
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
        info.setBroadcast(this.broadcast);
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

}
