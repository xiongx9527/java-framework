package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.producer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.script.annotation.Comment;

import java.beans.Transient;
import java.util.UUID;

@Slf4j
public class RabbitMqProducer {

    /**
     * 消息生产者的唯一标识符
     */
    private final String id;

    /**
     * 用于消息加密或认证的密钥
     */
    private final String key;

    /**
     * 消息生产者的名称
     */
    private final String name;

    /**
     * RabbitMQ消息发送模板
     */
    private final RabbitTemplate producer;

    /**
     * 默认发送模式
     */
    private static final boolean DEFAULT_ASYNC_MODE = true;

    /**
     * 构造函数，使用数据源信息创建RabbitMQ生产者
     */
    public RabbitMqProducer(RabbitMqDataSourceInfo dataSourceInfo) {
        this.producer = createProducer(dataSourceInfo);
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
    }

    /**
     * 构造函数，使用已有的RabbitTemplate创建生产者
     */
    public RabbitMqProducer(RabbitTemplate producer) {
        this.producer = producer;
        this.id = UUID.randomUUID().toString();
        this.name = Options.DEFAULT_DATA_SOURCE.getName();
        this.key = Options.DEFAULT_DATA_SOURCE.getValue();
        configureCallbacks(this.producer);
    }

    /**
     * 创建RabbitMQ连接工厂
     */
    @Transient
    private ConnectionFactory createConnectionFactory(RabbitMqDataSourceInfo dataSourceInfo) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        String host = dataSourceInfo.getHost();
        host = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "host", host);
        connectionFactory.setHost(host);
        String portStr = dataSourceInfo.getPort();
        portStr = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "port", portStr);
        int port = Integer.parseInt(portStr);
        connectionFactory.setPort(port);
        String username = dataSourceInfo.getUsername();
        username = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "username", username);
        String password = dataSourceInfo.getPassword() ;
        password = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "password", password);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            connectionFactory.setUsername(username);
            connectionFactory.setPassword(password);
        }
        String virtualHost = dataSourceInfo.getVirtualHost();
        virtualHost = MagicAPIConfigUtils.getRabbitMQConfig(dataSourceInfo.getKey(), "virtualHost", virtualHost);
        if (StringUtils.isNotBlank(virtualHost)) {
            connectionFactory.setVirtualHost(virtualHost);
        }
        // 开启发布者确认和返回机制
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
        return connectionFactory;
    }

    /**
     * 创建RabbitMQ生产者模板
     */
    @Transient
    public RabbitTemplate createProducer(RabbitMqDataSourceInfo dataSourceInfo) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(createConnectionFactory(dataSourceInfo));
        rabbitTemplate.setMandatory(true);
        configureCallbacks(rabbitTemplate);
        return rabbitTemplate;
    }

    /**
     * 配置确认和返回回调
     */
    private void configureCallbacks(RabbitTemplate rabbitTemplate) {
        // 消息确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData != null) {
                String messageId = correlationData.getId();
                if (ack) {
                    log.debug("消息发送确认成功，消息ID: {}", messageId);
                } else {
                    log.error("消息发送确认失败，消息ID: {}, 原因: {}", messageId, cause);
                }
            }
        });

        // 消息返回回调（无法路由时触发）
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息无法路由，交换机: {}, 路由键: {}, 回复码: {}, 回复文本: {}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyCode(),
                    returned.getReplyText());
        });
    }

    /**
     * ====================== 核心发送方法 ======================
     */

    /**
     * 发送消息到指定的交换机和路由键
     *
     * @param exchange   交换机名称，为空时使用默认交换机
     * @param routingKey 路由键
     * @param msg        消息内容
     * @param async      是否异步发送（RabbitMQ原生是异步的，此参数用于控制是否等待确认）
     * @return 发送是否成功（异步模式下总是返回true）
     */
    public boolean send(String exchange, String routingKey, Object msg, boolean async) {
        if (msg == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.rabbitmq.messageRequired"));
        }

        // 生成消息ID
        String messageId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(messageId);

        try {
            if (!async) {
                // 设置消息属性（包括traceId）
                this.producer.convertAndSend(
                        StringUtils.defaultIfBlank(exchange, ""),
                        StringUtils.defaultIfBlank(routingKey, ""),
                        msg,
                        message -> {
                            // 设置消息属性
                            message.getMessageProperties().setMessageId(messageId);
                            message.getMessageProperties().setCorrelationId(messageId);

                            return message;
                        },
                        correlationData
                );
            } else {
                Object result = this.producer.convertSendAndReceive(
                        StringUtils.defaultIfBlank(exchange, ""),
                        StringUtils.defaultIfBlank(routingKey, ""),
                        msg,
                        message -> {
                            // 设置消息属性
                            message.getMessageProperties().setMessageId(messageId);
                            message.getMessageProperties().setCorrelationId(messageId);

                            return message;
                        },
                        correlationData
                );
                if (result != null) {
                    log.debug("异步发送成功，消息ID: {}, 返回结果: {}", messageId, result);
                } else {
                    log.debug("异步发送成功，消息ID: {}", messageId);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("发送消息时发生异常，消息ID: {}, 交换机: {}, 路由键: {}, 异常信息: {}",
                    messageId, exchange, routingKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * ====================== 便捷发送方法 ======================
     */

    /**
     * 异步发送消息到默认交换机
     * @param msg 发送的消息
     * @return 发送结果，true表示发送成功，false表示发送失败
     */
    @Comment("异步发送消息到默认交换机")
    public boolean sendAsync(@Comment(name = "msg", value = "发送的消息") Object msg) {
        return send("", "", msg, true);
    }

    /**
     * 异步发送消息到默认交换机
     * @param routingKey 路由键
     * @param msg 发送的消息
     * @return 发送结果，true表示发送成功，false表示发送失败
     */
    @Comment("异步发送消息到默认交换机")
    public boolean sendAsync(@Comment(name = "routingKey", value = "路由键") String routingKey,
                             @Comment(name = "msg", value = "发送的消息") Object msg) {
        return send("", routingKey, msg, true);
    }

    /**
     * 异步发送消息
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param msg 发送的消息
     * @return 发送结果，true表示发送成功，false表示发送失败
     */
    @Comment("异步发送消息")
    public boolean sendAsync(@Comment(name = "exchange", value = "交换机") String exchange,
                             @Comment(name = "routingKey", value = "路由键") String routingKey,
                             @Comment(name = "msg", value = "发送的消息") Object msg) {
        return send(exchange, routingKey, msg, true);
    }

    /**
     * 同步发送消息到默认交换机
     * @param msg 发送的消息
     * @return 发送结果，true表示发送成功，false表示发送失败
     */
    @Comment("同步发送消息到默认交换机")
    public boolean send(@Comment(name = "msg", value = "发送的消息") Object msg) {
        return send("", "", msg, false);
    }

    /**
     * 同步发送消息到默认交换机
     * @param routingKey 路由键
     * @param msg 发送的消息
     * @return 发送结果，true表示发送成功，false表示发送失败
     */
    @Comment("同步发送消息到默认交换机")
    public boolean send(@Comment(name = "routingKey", value = "路由键") String routingKey,
                        @Comment(name = "msg", value = "发送的消息") Object msg) {
        return send("", routingKey, msg, false);
    }

    /**
     * 同步发送消息
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param msg 发送的消息
     * @return 发送结果，true表示发送成功，false表示发送失败
     */
    @Comment("同步发送消息")
    public boolean send(@Comment(name = "exchange", value = "交换机") String exchange,
                        @Comment(name = "routingKey", value = "路由键") String routingKey,
                        @Comment(name = "msg", value = "发送的消息") Object msg) {
        return send(exchange, routingKey, msg, false);
    }


    /**
     * ====================== 资源管理方法 ======================
     */

    /**
     * 关闭RabbitMQ生产者资源
     * 停止并销毁生产者实例，释放相关资源
     */
    @Transient
    public void close() {
        try {
            // 安全关闭生产者资源
            if (this.producer != null) {
                this.producer.stop();
                this.producer.destroy();
                log.info("RabbitMQ生产者已关闭，ID: {}", this.id);
            }
        } catch (Exception e) {
            log.warn("关闭RabbitMQ生产者时发生异常", e);
        }
    }

    /**
     * 获取生产者唯一标识
     *
     * @return 返回生产者的ID字符串
     */
    @Transient
    public String getId() {
        return this.id;
    }

    /**
     * 获取生产者名称
     *
     * @return 返回生产者的名称字符串
     */
    @Transient
    public String getName() {
        return this.name;
    }

    /**
     * 获取生产者的键值
     *
     * @return 返回生产者的键值字符串
     */
    @Transient
    public String getKey() {
        return this.key;
    }

    /**
     * 获取RabbitMQ生产者模板实例
     *
     * @return 返回RabbitTemplate生产者实例
     */
    @Transient
    public RabbitTemplate getProducer() {
        return this.producer;
    }

    /**
     * 检查生产者是否正在运行
     *
     * @return 如果生产者不为空且正在运行则返回true，否则返回false
     */
    @Transient
    public boolean isRunning() {
        return this.producer != null && this.producer.isRunning();
    }

}
