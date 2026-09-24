package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.RedisWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.redis.WsRedisMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 跨实例广播自动配置，由 {@link WsAutoConfiguration} 的 {@code @Import} 收编。
 *
 * <p>容器内存在 {@code StringRedisTemplate} 时启用：WebSocket 处理器换成
 * {@link RedisWebSocketHandler}，并注册 Redis 频道监听容器，使广播跨实例生效。
 * 未引入 Redis 时该配置不生效，退化为单机广播。</p>
 *
 * <p>{@code fw.plus.ws.enabled} 总开关由 {@link WsAutoConfiguration} 负责：它不通过时本类不会被解析，
 * 因此这里不再重复声明。</p>
 *
 * @author zzy
 */
@Configuration
@ConditionalOnClass(name = {"org.springframework.data.redis.core.StringRedisTemplate"})
@ConditionalOnBean(type = {"org.springframework.data.redis.core.StringRedisTemplate"})
@AutoConfigureAfter(name = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"})
public class WsRedisConfiguration {

	/**
	 * 基于 Redis 的 WebSocket 处理器。宿主已自行提供处理器时不再创建。
	 *
	 * @param wsConfig WebSocket 通用配置
	 * @return WebSocket 处理器
	 */
	@Bean
	@ConditionalOnMissingBean(AbstractWebSocketHandler.class)
	public RedisWebSocketHandler redisWebSocketHandler(WsConfig wsConfig) {
		return new RedisWebSocketHandler(wsConfig);
	}

	/**
	 * Redis 消息监听容器，用于处理其它实例广播过来的 WebSocket 消息。宿主已自行提供容器时不再创建。
	 *
	 * <p>处理器由 {@code redisWebSocketHandler} 提供时，容器在构造阶段绑定其广播监听器；
	 * 宿主自行提供处理器（不参与 Redis 广播）时容器不绑定监听器，因此这里不声明对
	 * {@code redisWebSocketHandler} 的硬依赖，改为按类型探测。</p>
	 *
	 * @param wsConfig          WebSocket 通用配置
	 * @param connectionFactory Redis 连接工厂
	 * @param handlerProvider   当前 WebSocket 处理器（可能缺失）
	 * @return 消息监听容器
	 */
	@Bean
	@ConditionalOnMissingBean(WsRedisMessageListenerContainer.class)
	public WsRedisMessageListenerContainer redisMessageListenerContainer(
			WsConfig wsConfig,
			RedisConnectionFactory connectionFactory,
			ObjectProvider<AbstractWebSocketHandler> handlerProvider) {
		return new WsRedisMessageListenerContainer(wsConfig, connectionFactory, handlerProvider);
	}
}
