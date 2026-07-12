package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth;

import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerUniqueIdService;
import org.tio.core.ChannelContext;

/**
 * 示例自定义 clientId，请按照自己的需求和业务进行扩展
 *
 * @author L.cm
 */
//@Configuration(proxyBeanMethods = false)
public class MqttUniqueIdService implements IMqttServerUniqueIdService {

	/**
	 * 获取客户端唯一标识符
	 *
	 * @param context 通道上下文对象
	 * @param clientId 客户端ID
	 * @param userName 用户名
	 * @param password 密码
	 * @return 返回唯一的客户端标识符，将替代MQTT客户端传入的clientId，请确保返回的uniqueId唯一
	 */
	@Override
	public String getUniqueId(ChannelContext context, String clientId, String userName, String password) {
		// 返回的 uniqueId 会替代 mqtt client 传过来的 clientId，请保证返回的 uniqueId 唯一。
		return clientId;
	}


}
