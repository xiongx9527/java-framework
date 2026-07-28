package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.container;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.config.ClusterConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.service.MagicNotifyListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.ssssssss.magicapi.core.model.MagicNotify;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.utils.JsonUtils;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MagicAPIRedisMessageListenerContainer extends RedisMessageListenerContainer {

    /**
     * 集群配置信息
     */
    private final ClusterConfig config;
    
    /**
     * Redis连接工厂
     */
    private final RedisConnectionFactory connectionFactory;
    
    /**
     * MagicAPI服务实例
     */
    private final MagicAPIService magicAPIService;

    private final List<MagicNotifyListener> notifyListeners;

    /**
     * 构造函数，初始化Redis消息监听容器
     *
     * @param config             集群配置信息，包含Redis channel等配置
     * @param connectionFactory  Redis连接工厂，用于创建Redis连接
     * @param magicAPIService    MagicAPI服务实例，用于处理集群通知
     */
    public MagicAPIRedisMessageListenerContainer(ClusterConfig config,
                                                  RedisConnectionFactory connectionFactory,
                                                  MagicAPIService magicAPIService) {
        this(config, connectionFactory, magicAPIService, Collections.emptyList());
    }

    public MagicAPIRedisMessageListenerContainer(ClusterConfig config,
                                                  RedisConnectionFactory connectionFactory,
                                                  MagicAPIService magicAPIService,
                                                  List<MagicNotifyListener> notifyListeners) {
        this.config = config;
        this.connectionFactory = connectionFactory;
        this.magicAPIService = magicAPIService;
        this.notifyListeners = notifyListeners == null ? Collections.emptyList() : notifyListeners;
    }

    /**
     * 初始化方法，在Bean构造完成后自动执行
     * 设置Redis连接工厂并注册消息监听器，监听指定的Redis频道以接收集群通知
     */
    @PostConstruct
    public void init() {
        String channel = config.getChannel();
        log.info("开启集群通知监听，Redis channel: {}", channel);
        setConnectionFactory(connectionFactory);
        addMessageListener((message, pattern) -> {
                    MagicNotify notify = JsonUtils.readValue(message.getBody(), MagicNotify.class);
                    for (MagicNotifyListener listener : notifyListeners) {
                        listener.onNotify(notify);
                    }
                    magicAPIService.processNotify(notify);
                },
                ChannelTopic.of(channel));
    }
}
