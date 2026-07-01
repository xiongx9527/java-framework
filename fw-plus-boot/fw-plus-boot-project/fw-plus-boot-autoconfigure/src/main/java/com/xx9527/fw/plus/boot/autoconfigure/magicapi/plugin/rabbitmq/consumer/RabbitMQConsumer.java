package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeScriptInfo;
import com.xx9527.fw.plus.common.util.NetUtils;
import com.xx9527.fw.plus.common.util.SpringUtils;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RabbitMQConsumer implements BaseService {
    /** 消费者唯一标识 */
    private final String id;
    /** 消费者键值，用于配置关联 */
    private final String key;
    /** 消费者名称 */
    private final String name;
    /** 存储队列名与消息监听容器的映射关系，支持并发访问 */
    private final Map<String, SimpleMessageListenerContainer> consumerMap = new ConcurrentHashMap<>();

    /** RabbitMQ消费者服务，处理具体的消息消费逻辑 */
    private final RabbitMqConsumerService rabbitMqConsumerService;
    /** RabbitMQ数据源配置信息 */
    private final RabbitMqDataSourceInfo dataSourceInfo;

    /**
     * 构造函数，初始化RabbitMQ消费者实例
     * @param rabbitMqConsumerService RabbitMQ消费者服务，用于提供消费相关的业务逻辑
     * @param dataSourceInfo 数据源信息，包含连接RabbitMQ所需的基本配置信息
     * @param subscribeScriptInfos 订阅脚本信息列表，定义了需要订阅的消息队列及处理逻辑
     */
    public RabbitMQConsumer(RabbitMqConsumerService rabbitMqConsumerService, RabbitMqDataSourceInfo dataSourceInfo, List<RabbitMqSubscribeScriptInfo> subscribeScriptInfos) {
        this.rabbitMqConsumerService = rabbitMqConsumerService;
        this.dataSourceInfo = dataSourceInfo;
        this.consumerMap.putAll(createConsumer(rabbitMqConsumerService, dataSourceInfo, subscribeScriptInfos));
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
    }

    /**
     * 创建RabbitMQ连接工厂
     * 根据数据源信息配置连接参数，包括主机地址、端口、用户名、密码和虚拟主机
     * @param dataSourceInfo 数据源信息，包含连接RabbitMQ服务器所需的配置参数
     * @return 配置好的CachingConnectionFactory实例
     */
    private ConnectionFactory createConnectionFactory(RabbitMqDataSourceInfo dataSourceInfo) {
        String host = dataSourceInfo.getHost();
        host = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "host", host);
        String portStr = dataSourceInfo.getPort();
        portStr = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "port", portStr);
        int port = Integer.parseInt(portStr);
        String username = dataSourceInfo.getUsername();
        username = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "username", username);
        String password = dataSourceInfo.getPassword();
        password = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "password", password);
        String virtualHost = dataSourceInfo.getVirtualHost();
        virtualHost = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "virtualHost", virtualHost);
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        return connectionFactory;
    }

    /**
     * 配置 SimpleMessageListenerContainer 的各项消费者参数。
     *
     * @param simpleMessageListenerContainer 消息监听容器实例，用于设置各种消费者相关的配置
     * @param otherMap 包含配置项的键值对映射，支持多种可选配置参数
     */
    private void configureConsumer(SimpleMessageListenerContainer simpleMessageListenerContainer, Map<String, Object> otherMap) {
        // 设置消费者最大并发数
        if (otherMap.containsKey("maxConcurrentConsumers")) {
            Integer maxConcurrentConsumers = (Integer) otherMap.get("maxConcurrentConsumers");
            simpleMessageListenerContainer.setMaxConcurrentConsumers(maxConcurrentConsumers);
        }

        // 设置消费者启动与停止的时间间隔控制参数
        if (otherMap.containsKey("startConsumerMinInterval")) {
            Long startConsumerMinInterval = (Long) otherMap.get("startConsumerMinInterval");
            simpleMessageListenerContainer.setStartConsumerMinInterval(startConsumerMinInterval);
        }

        if (otherMap.containsKey("stopConsumerMinInterval")) {
            Long stopConsumerMinInterval = (Long) otherMap.get("stopConsumerMinInterval");
            simpleMessageListenerContainer.setStopConsumerMinInterval(stopConsumerMinInterval);
        }

        if (otherMap.containsKey("consecutiveActiveTrigger")) {
            Integer consecutiveActiveTrigger = (Integer) otherMap.get("consecutiveActiveTrigger");
            simpleMessageListenerContainer.setConsecutiveActiveTrigger(consecutiveActiveTrigger);
        }

        if (otherMap.containsKey("consecutiveIdleTrigger")) {
            Integer consecutiveIdleTrigger = (Integer) otherMap.get("consecutiveIdleTrigger");
            simpleMessageListenerContainer.setConsecutiveIdleTrigger(consecutiveIdleTrigger);
        }

        // 设置消息接收超时时间（单位：毫秒）
        if (otherMap.containsKey("receiveTimeout")) {
            Long receiveTimeout = (Long) otherMap.get("receiveTimeout");
            simpleMessageListenerContainer.setReceiveTimeout(receiveTimeout);
        }

        // 批量处理相关配置
        if (otherMap.containsKey("batchSize")) {
            Integer batchSize = (Integer) otherMap.get("batchSize");
            simpleMessageListenerContainer.setBatchSize(batchSize);
        }

        if (otherMap.containsKey("consumerBatchEnabled")) {
            Boolean consumerBatchEnabled = (Boolean) otherMap.get("consumerBatchEnabled");
            simpleMessageListenerContainer.setConsumerBatchEnabled(consumerBatchEnabled);
        }

        // 队列声明失败是否致命、重试次数及间隔等配置
        if (otherMap.containsKey("missingQueuesFatal")) {
            Boolean missingQueuesFatal = (Boolean) otherMap.get("missingQueuesFatal");
            simpleMessageListenerContainer.setMissingQueuesFatal(missingQueuesFatal);
        }

        if (otherMap.containsKey("declarationRetries")) {
            Integer declarationRetries = (Integer) otherMap.get("declarationRetries");
            simpleMessageListenerContainer.setDeclarationRetries(declarationRetries);
        }

        if (otherMap.containsKey("retryDeclarationInterval")) {
            Long retryDeclarationInterval = (Long) otherMap.get("retryDeclarationInterval");
            simpleMessageListenerContainer.setRetryDeclarationInterval(retryDeclarationInterval);
        }

        if (otherMap.containsKey("consumerStartTimeout")) {
            Long consumerStartTimeout = (Long) otherMap.get("consumerStartTimeout");
            simpleMessageListenerContainer.setConsumerStartTimeout(consumerStartTimeout);
        }

        // 设置默认拒绝策略：是否将被拒绝的消息重新入队
        if (otherMap.containsKey("defaultRequeueRejected")) {
            Boolean defaultRequeueRejected = (Boolean) otherMap.get("defaultRequeueRejected");
            simpleMessageListenerContainer.setDefaultRequeueRejected(defaultRequeueRejected);
        }

        // 设置消费者最小并发数量，默认为3
        Integer concurrentConsumers = (Integer) otherMap.getOrDefault("concurrentConsumers", 3);
        simpleMessageListenerContainer.setConcurrentConsumers(concurrentConsumers);

        // 再次设置默认拒绝策略（使用默认值false），防止前面未设置的情况
        Boolean defaultRequeueRejected = (Boolean) otherMap.getOrDefault("defaultRequeueRejected", false);
        simpleMessageListenerContainer.setDefaultRequeueRejected(defaultRequeueRejected);

        // 设置消息确认模式，默认为手动确认
        String acknowledgeModeStr = (String) otherMap.getOrDefault("acknowledgeMode", "MANUAL");
        AcknowledgeMode acknowledgeMode = null;
        if ("MANUAL".equalsIgnoreCase(acknowledgeModeStr)) {
            acknowledgeMode = AcknowledgeMode.MANUAL;
        } else if ("AUTO".equalsIgnoreCase(acknowledgeModeStr)) {
            acknowledgeMode = AcknowledgeMode.AUTO;
        } else if ("NONE".equalsIgnoreCase(acknowledgeModeStr)) {
            acknowledgeMode = AcknowledgeMode.NONE;
        }
        simpleMessageListenerContainer.setAcknowledgeMode(acknowledgeMode);

        // 设置消费者是否以独占方式连接队列，默认为非独占
        Boolean exclusive = (Boolean) otherMap.getOrDefault("exclusive", false);
        simpleMessageListenerContainer.setExclusive(exclusive);

        // 设置每个消费者预取消息的数量，默认为1，实现更公平的消息分发
        Integer prefetchCount = (Integer) otherMap.getOrDefault("prefetchCount", 1);
        simpleMessageListenerContainer.setPrefetchCount(prefetchCount);

        // 启动容器自动启动功能
        Boolean autoStartup = (Boolean) otherMap.getOrDefault("autoStartup", true);
        simpleMessageListenerContainer.setAutoStartup(autoStartup);
    }

    /**
     * 创建消息消费者
     *
     * @param subscribeScriptInfos 订阅脚本信息列表，用于配置消费者的信息
     */
    public void createConsumer(List<RabbitMqSubscribeScriptInfo> subscribeScriptInfos) {
        // 检查订阅脚本信息列表是否为空，如果为空则直接返回
        if (CollectionUtils.isEmpty(subscribeScriptInfos)) {
            return;
        }
        // 调用具体的创建消费者方法，并将结果存入consumerMap中
        this.consumerMap.putAll(createConsumer(this.rabbitMqConsumerService, this.dataSourceInfo, subscribeScriptInfos));
    }

    /**
     * 创建并配置一个 RabbitMQ 消费者容器（SimpleMessageListenerContainer）。
     *
     * @param rabbitMqConsumerService 消息消费服务实例，用于处理接收到的消息及管理订阅脚本信息
     * @param dataSourceInfo 数据源连接信息，包括主机、端口、用户名、密码等，用于构建 ConnectionFactory
     * @param subscribeScriptInfos 订阅脚本信息列表，包含队列名称及其他消费相关配置
     * @return 配置完成的 SimpleMessageListenerContainer 实例，可用于启动消息监听
     */
    private Map<String,SimpleMessageListenerContainer> createConsumer(RabbitMqConsumerService rabbitMqConsumerService, RabbitMqDataSourceInfo dataSourceInfo, List<RabbitMqSubscribeScriptInfo> subscribeScriptInfos) {
        Map<String,SimpleMessageListenerContainer> consumerMap = new ConcurrentHashMap<>();
        for (RabbitMqSubscribeScriptInfo info : subscribeScriptInfos) {
            Boolean enable = enable(info);
            if (!Boolean.TRUE.equals(enable)) {
                continue;
            }
            // 创建消息监听容器，并设置连接工厂
            SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer(createConnectionFactory(dataSourceInfo));
            // 设置监听的队列名称列表
            simpleMessageListenerContainer.setQueueNames(info.getQueueName());
            if (info.isBroadcast()) {
                String serviceName = SpringUtils.getApplicationName();
                String serverPort = SpringUtils.getServerPort();
                String macAddress = NetUtils.getMacAddress();
                String queueName = info.getQueueName() ;
                queueName += "-broadcast-" + serviceName + "-" + serverPort + "-" + macAddress;
                simpleMessageListenerContainer.setQueueNames(queueName);
            }
            String other = info.getOther();
            other = MagicAPIConfigUtils.getRabbitMQSubscribeConfig(info.getKey(), info.getQueueName(), "other", other);
            Map<String, Object> otherMap = new HashMap<>();
            if (StringUtils.isNotBlank(other)) {
                otherMap.putAll(JSON.parseObject(other, new TypeReference<Map<String, Object>>() {}));
            }

            configureConsumer(simpleMessageListenerContainer, otherMap);
            // 从 otherMap 获取重试配置
            Integer maxRetries = (Integer) otherMap.getOrDefault("maxRetries", 3);
            Long retryInterval = (Long) otherMap.getOrDefault("retryInterval", 5000L);

            // 注册自定义消息监听处理器
            simpleMessageListenerContainer.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
                try {
                    boolean success = rabbitMqConsumerService.consume(dataSourceInfo.getKey(), message);
                    if (success) {
                        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
                    } else {
                        throw new RuntimeException("Consumption failed for message");
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    try {
                        handleRetryOrReject(channel, message, maxRetries, retryInterval);
                    } catch (IOException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            });
            this.consumerMap.put(info.getQueueName(), simpleMessageListenerContainer);
        }
        return consumerMap;
    }

    /**
     * 处理消息的重试或拒绝逻辑
     *
     * @param channel RabbitMQ通道对象，用于执行消息确认操作
     * @param message 需要处理的消息对象
     * @param maxRetries 最大重试次数
     * @param retryInterval 重试间隔时间（毫秒）
     * @throws IOException 当RabbitMQ操作失败时抛出
     */
    private void handleRetryOrReject(Channel channel, Message message, Integer maxRetries, Long retryInterval) throws IOException {
        // 检查是否已达到最大重试次数
        Integer retryCount = (Integer) message.getMessageProperties().getHeaders().getOrDefault("retry-count", 0);
        if (retryCount < maxRetries) {
            // 增加重试次数并重新入队
            message.getMessageProperties().getHeaders().put("retry-count", retryCount + 1);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        } else {
            // 达到最大重试次数，拒绝消息
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }


    /**
     * 关闭消费者资源
     * 依次调用消费者的stop、shutdown和destroy方法来释放资源
     */
    public void close() {
        for (SimpleMessageListenerContainer consumer : consumerMap.values()) {
            try {
                consumer.stop();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            try {
                consumer.shutdown();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            try {
                consumer.destroy();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * 获取消费者ID
     * @return 返回消费者的唯一标识符
     */
    public String getId() {
        return this.id;
    }

    /**
     * 获取消费者名称
     * @return 返回消费者的名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 获取消费者键值
     * @return 返回消费者的键值
     */
    public String getKey() {
        return this.key;
    }

    /**
     * 取消订阅RabbitMQ队列
     * 从消费者监听列表中移除指定队列，并从订阅脚本信息映射中删除对应信息
     *
     * @param subscribeScriptInfo 订阅信息对象，包含要取消订阅的队列名称和键值等信息
     */
    public void unsubscribe(RabbitMqSubscribeInfo subscribeScriptInfo) {
        if (Objects.isNull(subscribeScriptInfo)) {
            return;
        }

        String queueName = subscribeScriptInfo.getQueueName();
        if (StringUtils.isBlank(queueName)) {
            return;
        }

        SimpleMessageListenerContainer consumer = this.consumerMap.remove(queueName);
        if (consumer != null) {
            try {
                consumer.stop();
            } catch (Exception e) {
                // 日志记录 stop 失败
            }
            try {
                consumer.shutdown();
            } catch (Exception e) {
                // 日志记录 shutdown 失败
            }
            try {
                consumer.destroy();
            } catch (Exception e) {
                // 日志记录 destroy 失败
            }
        }
    }


}
