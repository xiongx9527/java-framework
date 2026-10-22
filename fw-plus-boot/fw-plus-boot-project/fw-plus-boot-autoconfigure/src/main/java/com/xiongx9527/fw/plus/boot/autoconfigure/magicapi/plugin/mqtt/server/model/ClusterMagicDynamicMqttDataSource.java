package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDownReceiver;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageExchangeReceiver;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageStore;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.enums.RedisKeys;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.listener.RedisMessageListener;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerAuthHandler;
import org.dromara.mica.mqtt.core.server.pipeline.MqttPublishPipelineHandler;
import org.dromara.mica.mqtt.core.server.serializer.JsonMessageSerializer;
import org.dromara.mica.mqtt.core.server.session.IMqttSessionManager;
import org.dromara.mica.mqtt.core.server.session.InMemoryMqttSessionManager;
import org.dromara.mica.mqtt.core.server.store.IMqttMessageStore;
import net.dreamlu.mica.redis.cache.MicaRedisCache;
import net.dreamlu.mica.redis.stream.DefaultRStreamTemplate;
import net.dreamlu.mica.redis.stream.RStreamTemplate;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.Map;

public class ClusterMagicDynamicMqttDataSource extends BaseMagicDynamicMqttDataSource implements ApplicationContextAware {


    /**
     * JSON消息序列化器实例
     * 用于将消息对象序列化为JSON格式或从JSON格式反序列化为消息对象
     */
    private final JsonMessageSerializer jsonMessageSerializer = new JsonMessageSerializer();

    /**
     * Redis操作模板类实例
     *
     * 用于执行Redis相关的操作，支持字符串类型的键和对象类型的值
     * 提供了对Redis的各种数据结构操作的封装
     */
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 默认构造函数，用于创建ClusterMagicDynamicMqttDataSource实例
     */
    public ClusterMagicDynamicMqttDataSource() {}

    /**
     * 带RedisTemplate参数的构造函数，用于创建ClusterMagicDynamicMqttDataSource实例
     * @param redisTemplate Redis模板对象，用于与Redis进行数据交互
     */
    public ClusterMagicDynamicMqttDataSource(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取MagicAPI MQTT服务器唯一ID服务实例
     *
     * @param key 用于标识MQTT服务器的键值
     * @return 返回新的MagicAPIMqttServerUniqueIdService实例
     */
    @Override
    public MagicAPIMqttServerUniqueIdService getMagicAPIMqttServerUniqueIdService(String key) {
        return new MagicAPIMqttServerUniqueIdService(key);
    }

    /**
     * 获取MagicAPI MQTT连接状态监听器实例
     *
     * @param key 用于标识连接的键值
     * @return 返回新的MagicAPIMqttConnectStatusListener实例
     */
    @Override
    public MagicAPIMqttConnectStatusListener getMagicAPIMqttConnectStatusListener(String key) {
        return new MagicAPIMqttConnectStatusListener(key);
    }


    /**
     * 获取MQTT消息交换接收器实例
     *
     * @param mqttServer MQTT服务器实例
     * @param key 用于构建Redis通道名称的键值
     * @return MQTT消息交换接收器实例
     */
    @Override
    public RedisMqttMessageDownReceiver getMqttMessageExchangeReceiver(MqttServer mqttServer, String key) {
        // 构建Redis通道名称
        String channel = RedisKeys.REDIS_CHANNEL_DOWN.getKey()+":"+key;
        RedisMqttMessageDownReceiver redisMqttMessageDownReceiver = new RedisMqttMessageDownReceiver(jsonMessageSerializer, mqttServer, channel,redisTemplate.getConnectionFactory());
        // 下行通道不做来源节点过滤
        redisMqttMessageDownReceiver.addMessageListener(new RedisMessageListener(jsonMessageSerializer, mqttServer),new ChannelTopic(channel));

        return redisMqttMessageDownReceiver;
    }

    /**
     * 获取MQTT下行消息接收器实例
     *
     * @param mqttServer MQTT服务器实例
     * @param key 用于构建Redis通道名称的键值
     * @return MQTT下行消息接收器实例
     */
    @Override
    public RedisMqttMessageExchangeReceiver getMqttMessageDownReceiver(MqttServer mqttServer, String key) {
        // 构建Redis通道名称
        String channel = RedisKeys.REDIS_CHANNEL_EXCHANGE.getKey()+":"+key;
        RedisMqttMessageExchangeReceiver redisMqttMessageExchangeReceiver = new RedisMqttMessageExchangeReceiver(jsonMessageSerializer, mqttServer, channel,redisTemplate.getConnectionFactory());
        // 上行交换通道：来源节点已由发布管线完成本地投递，本站点跳过，避免重复下发
        redisMqttMessageExchangeReceiver.addMessageListener(new RedisMessageListener(jsonMessageSerializer, mqttServer, mqttServer.getServerCreator().getNodeName()),new ChannelTopic(channel));
        return redisMqttMessageExchangeReceiver;
    }

    /**
     * 获取MQTT服务器认证处理器
     *
     * @param key 认证处理器的键值标识
     * @return 返回IMqttServerAuthHandler认证处理器实例，如果获取失败则返回null
     */
    @Override
    public IMqttServerAuthHandler getIMqttServerAuthHandler(String key) {
        try {
            // 从Spring容器中获取IMqttServerAuthHandler类型的Bean实例
            return SpringUtils.getBean(IMqttServerAuthHandler.class);
        } catch (Exception e) {
            // 捕获异常，避免因获取Bean失败导致程序中断
        }
        return null;
    }


    /**
     * 获取MQTT发布管线处理器实例
     *
     * <p>集群模式：在本地投递（内建管线）之后，把上行消息转发到 Redis 交换通道，
     * 由其它节点消费并投递给各自本地的订阅者。</p>
     *
     * @param key 用于构建Redis通道名称的键值
     * @return 返回配置好的Redis MQTT发布管线处理器实例
     */
    @Override
    public MqttPublishPipelineHandler getMqttPublishPipelineHandler(String key) {
        // 创建Redis流模板实例用于消息队列操作
        RStreamTemplate streamTemplate = new DefaultRStreamTemplate(redisTemplate);
        // 构建Redis通道名称，格式为：通道前缀:键值
        String channel = RedisKeys.REDIS_CHANNEL_EXCHANGE.getKey()+":"+key;
        // 创建并配置Redis MQTT消息分发器（发布管线处理器）
        RedisMqttMessageDispatcher redisMqttMessageDispatcher = new RedisMqttMessageDispatcher(streamTemplate, jsonMessageSerializer, channel);
        return redisMqttMessageDispatcher;
    }


    /**
     * 获取MQTT会话管理器实例
     *
     * @param key 用于标识会话管理器的键值
     * @return 返回基于Redis实现的MQTT会话管理器实例
     */
    @Override
    public IMqttSessionManager getIMqttSessionManager(String key) {
        return new InMemoryMqttSessionManager();
    }

    /**
     * 获取MQTT消息存储实例
     *
     * @param key 用于标识消息存储的键值
     * @return 返回基于Redis实现的MQTT消息存储实例
     */
    @Override
    public IMqttMessageStore getIMqttMessageStore(String key) {
        MicaRedisCache micaRedisCache = new MicaRedisCache(redisTemplate);
        RedisMqttMessageStore redisMqttMessageStore = new RedisMqttMessageStore(jsonMessageSerializer, micaRedisCache,key);
        return redisMqttMessageStore;
    }


    /**
     * 设置应用上下文，并从中获取RedisTemplate实例
     *
     * @param applicationContext 应用上下文对象
     * @throws BeansException 当Bean处理出现异常时抛出
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (this.redisTemplate != null) {
            return ;
        }
        // 从应用上下文中获取所有RedisTemplate类型的Bean实例
        Map<String, RedisTemplate> map = MapUtils.emptyIfNull(applicationContext.getBeansOfType(RedisTemplate.class));
        for (Map.Entry<String, RedisTemplate> entry : map.entrySet()) {
            // 跳过StringRedisTemplate类型的实例，只获取RedisTemplate实例
            if (entry.getValue() instanceof StringRedisTemplate) {
                continue;
            }
            this.redisTemplate = entry.getValue();
            break;
        }
        // 如果未找到合适的RedisTemplate实例，则抛出运行时异常
        if (this.redisTemplate == null) {
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.task.redisTemplateNotFound"));
        }
    }



}
