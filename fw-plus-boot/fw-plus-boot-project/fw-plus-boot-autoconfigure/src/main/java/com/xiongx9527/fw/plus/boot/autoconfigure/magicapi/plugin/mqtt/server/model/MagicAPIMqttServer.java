package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDownReceiver;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageExchangeReceiver;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.core.server.MqttServer;

import java.util.Objects;


@Slf4j
public class MagicAPIMqttServer {

    /** MQTT服务器实例，用于处理MQTT协议相关的操作 */
    private final MqttServer mqttServer;

    /** Redis下行消息接收器，负责接收从Redis发送到MQTT的下行消息 */
    private final RedisMqttMessageDownReceiver redisMqttMessageDownReceiver;

    /** Redis交换消息接收器，负责处理Redis与MQTT之间的消息交换 */
    private final RedisMqttMessageExchangeReceiver redisMqttMessageExchangeReceiver;

    /**
     * 构造函数，用于创建MagicApiMqttServer实例
     * @param mqttServer MQTT服务器实例，用于处理MQTT协议相关的操作
     * @param redisMqttMessageDownReceiver Redis下行消息接收器，用于接收从Redis发送到MQTT的下行消息
     * @param redisMqttMessageExchangeReceiver Redis交换消息接收器，用于处理Redis与MQTT之间的消息交换
     */
    public MagicAPIMqttServer(MqttServer mqttServer, RedisMqttMessageDownReceiver redisMqttMessageDownReceiver, RedisMqttMessageExchangeReceiver redisMqttMessageExchangeReceiver) {
        this.mqttServer = mqttServer;
        this.redisMqttMessageDownReceiver = redisMqttMessageDownReceiver;
        this.redisMqttMessageExchangeReceiver = redisMqttMessageExchangeReceiver;
    }

    /**
     * 获取MQTT服务器实例
     * @return 返回当前配置的MqttServer实例
     */
    public MqttServer getMqttServer() {
        return this.mqttServer;
    }


    /**
     * 启动MQTT服务及相关组件
     *
     * 该方法按顺序启动以下组件：
     * 1. MQTT服务器
     * 2. Redis下行消息接收器
     * 3. Redis交换消息接收器
     *
     * 如果任意组件启动失败，会尝试停止已启动的前置组件，并抛出运行时异常
     */
    public void start() {
        // 启动MQTT服务器
        if (Objects.nonNull(this.mqttServer)) {
            try {
                this.mqttServer.start();
            } catch (Exception e) {
                throw new RuntimeException("MQTT服务器启动失败", e);
            }
        }

        // 启动Redis下行消息接收器
        if (Objects.nonNull(this.redisMqttMessageDownReceiver)) {
            try {
                this.redisMqttMessageDownReceiver.start();
            } catch (Exception e) {
                // 启动失败时，停止已启动的MQTT服务器
                if (Objects.nonNull(this.mqttServer)) {
                    try {
                        this.mqttServer.stop();
                    } catch (Exception e1) {
                    }
                }
                throw new RuntimeException("Redis下行消息接收器启动失败", e);
            }
        }

        // 启动Redis交换消息接收器
        if (Objects.nonNull(this.redisMqttMessageExchangeReceiver)) {
            try {
                this.redisMqttMessageExchangeReceiver.start();
            } catch (Exception e) {
                // 启动失败时，停止已启动的MQTT服务器和Redis下行消息接收器
                if (Objects.nonNull(this.mqttServer)) {
                    try {
                        this.mqttServer.stop();
                    } catch (Exception e1) {
                    }
                }
                if (Objects.nonNull(this.redisMqttMessageDownReceiver)) {
                    try {
                        this.redisMqttMessageDownReceiver.stop();
                    } catch (Exception e1) {
                    }
                }
                throw new RuntimeException("Redis交换消息接收器启动失败", e);
            }
        }

    }



    /**
     * 停止MQTT服务及相关资源
     *
     * 该方法用于安全地停止MQTT服务器以及相关的Redis消息接收器组件。
     * 在停止过程中，即使某个组件停止失败，也会继续尝试停止其他组件，
     * 确保尽可能多地释放系统资源。
     */
    public void stop() {
        // 停止MQTT服务器
        if (Objects.nonNull(this.mqttServer)) {
            try {
                this.mqttServer.stop();
            } catch (Exception e) {
                // 记录MQTT服务器停止异常，但不中断其他资源的停止过程
                log.error("Failed to stop MQTT server: " + e.getMessage());
            }
        }

        // 停止Redis下行消息接收器
        if (Objects.nonNull(this.redisMqttMessageDownReceiver)) {
            try {
                this.redisMqttMessageDownReceiver.stop();
            } catch (Exception e) {
                // 记录Redis下行消息接收器停止异常
                log.error("Failed to stop Redis MQTT message down receiver: " + e.getMessage());
            }
        }

        // 停止Redis交换消息接收器
        if (Objects.nonNull(this.redisMqttMessageExchangeReceiver)) {
            try {
                this.redisMqttMessageExchangeReceiver.stop();
            } catch (Exception e) {
                // 记录Redis交换消息接收器停止异常
                log.error("Failed to stop Redis MQTT message exchange receiver: " + e.getMessage());
            }
        }
    }

}
