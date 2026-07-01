package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.starter.MagicAPIMqttConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

import java.util.Map;

public interface BaseService extends MagicBaseService {


    /**
     * 验证MQTT数据源信息实体的字段有效性
     *
     * @param entity MQTT数据源信息实体对象，包含需要验证的各个字段
     */
    default void validateField(MqttDataSourceInfo entity) {
        // 验证必填字段：确保名称、密钥、主机和协议都不为空
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getIp(), "ip");
        validateRequiredField(entity.getPort(), "port");
        validateRequiredField(entity.getWebPort(), "webPort");
        // 如果配置内容不为空，则进行JSON格式校验
        validateJSON(entity.getOther(),"其它配置");
    }


        /**
     * 获取MQTT配置信息
     *
     * @return 返回MQTT配置对象，包含MQTT连接相关配置参数
     */
    default Map<String, Object> getConfig() {
        // 从Spring容器中获取MQTT配置Bean实例
        return SpringUtils.getBean(MagicAPIMqttConfig.class);
    }


    /**
     * 获取心跳超时时间配置值
     *
     * @return 心跳超时时间，单位为秒。如果配置中未指定，则返回默认值60秒
     */
    default long getHeartbeatTimeout() {
        return getConfigValueAsLong(getConfig(), "heartbeatTimeout", 60);
    }

        /**
     * 获取keepalive退避因子配置值
     *
     * @return keepalive退避因子，如果未配置则返回默认值1.5f
     */
    default float getKeepaliveBackoff() {
        return getConfigValueAsFloat(getConfig(), "keepaliveBackoff", 1.5f);
    }

    /**
     * 获取客户端ID的最大长度限制
     *
     * @return 返回配置中指定的最大客户端ID长度，如果未配置则返回默认值50
     */
    default int getMaxClientIdLength() {
        return getConfigValueAsInt(getConfig(), "maxClientIdLength", 50);
    }

    /**
     * 获取读取缓冲区大小配置值
     *
     * @return 返回读取缓冲区的大小，单位为字节。如果配置中未指定该值，则返回默认值1024字节
     */
    default long getReadBufferSize() {
        return getConfigValueAsLong(getConfig(), "readBufferSize", 1024);
    }


    /**
     * 获取消息最大字节数配置值
     *
     * @return 返回单个消息允许的最大字节数。如果配置中未指定该值，则返回默认值1048576字节（1MB）
     */
    default long getMaxBytesInMessage() {
        return getConfigValueAsLong(getConfig(), "maxBytesInMessage", 1024 * 1024);
    }


    /**
     * 获取时间轮的tick间隔毫秒数
     *
     * @return tick间隔毫秒数，默认值为200L
     */
    default long getTickMs() {
        return getConfigValueAsLong(getConfig(), "tickMs", 200L);
    }


    /**
     * 获取时间轮的轮子大小
     *
     * @return 轮子大小，默认值为60
     */
    default int getWheelSize() {
        return getConfigValueAsInt(getConfig(), "wheelSize", 60);
    }



}
