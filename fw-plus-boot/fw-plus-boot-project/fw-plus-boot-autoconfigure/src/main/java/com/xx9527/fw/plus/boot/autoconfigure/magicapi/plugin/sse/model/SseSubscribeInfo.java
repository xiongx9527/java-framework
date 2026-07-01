package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

/**
 * 订阅信息
 *
 * @author zzy
 */
@Data
public class SseSubscribeInfo extends PathMagicEntity {

    /**
     * 数据源Key
     */
    private String key;

    /**
     * 业务标识
     */
    private String businessId;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 描述
     */
    private String description;

    /**
     * 将当前对象的属性复制到指定的目标对象中
     *
     * @param info 目标SseSubscribeInfo对象，用于接收当前对象的属性值
     * @return 复制了当前对象属性的目标对象
     */
    public SseSubscribeInfo copyTo(SseSubscribeInfo info) {
        super.copyTo(info);
        info.setKey(this.key);
        info.setEnabled(this.enabled);
        info.setDescription(this.description);
        info.setBusinessId(this.businessId);
        info.setPath(null);
        return info;
    }

    /**
     * 创建并返回当前对象的一个副本
     *
     * @return 新创建的SseSubscribeInfo对象，包含与当前对象相同的属性值
     */
    @Override
    public SseSubscribeInfo copy() {
        SseSubscribeInfo info = new SseSubscribeInfo();
        this.copyTo(info);
        return info;
    }

    /**
     * 创建并返回当前对象的简化版本
     *
     * @return 新创建的MagicEntity对象，实际返回的是SseSubscribeInfo实例
     */
    @Override
    public MagicEntity simple() {
        SseSubscribeInfo info = new SseSubscribeInfo();
        this.copyTo(info);
        return info;
    }


}
