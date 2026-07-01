package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerUniqueIdService;
import org.tio.core.ChannelContext;

public class MagicAPIMqttServerUniqueIdService implements IMqttServerUniqueIdService {

    /**
     * 私有的不可变字符串变量，用于存储键值
     */
    private final String key ;

    /**
     * 构造函数，初始化MagicAPIMqttServerUniqueIdService实例
     * @param key 用于生成唯一ID的键值
     */
    public MagicAPIMqttServerUniqueIdService(String key) {
        this.key = key;
    }

    /**
     * 获取MQTT客户端的唯一标识符
     * 通过将服务键值与客户端ID组合生成唯一标识
     * @param context 通道上下文对象
     * @param clientId 客户端ID
     * @param userName 用户名
     * @param password 密码
     * @return 返回由key和clientId组成的唯一标识符，格式为"key:clientId"
     */
    @Override
    public String getUniqueId(ChannelContext context, String clientId, String userName, String password) {
        return key + ":" + clientId;
    }

}
