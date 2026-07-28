package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.producer;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.utils.JsonUtils;
import org.ssssssss.script.annotation.Comment;

import java.beans.Transient;
import java.util.UUID;


@Slf4j
public class RocketMqProducer {

    /**
     * 消息队列生产者实例的唯一标识符
     */
    private final String id;

    /**
     * 消息队列生产者的访问密钥
     */
    private final String key;

    /**
     * 消息队列生产者的名称
     */
    private final String name;


    /**
     * 数据源信息，包含连接 RocketMQ 所需的配置
     */
    private final RocketMqDataSourceInfo dataSourceInfo;

    /**
     * RocketMQ消息生产者对象，用于发送消息到消息队列
     */
    private final DefaultMQProducer producer;


    /**
     * 构造函数，使用默认的 RocketMQ 生产者实例初始化 RocketMqProducer。
     *
     * @param producer 默认的 RocketMQ 生产者实例
     */
    public RocketMqProducer(DefaultMQProducer producer) {
        this.producer = producer;
        this.id = UUID.randomUUID().toString();
        this.name = Options.DEFAULT_DATA_SOURCE.getName();
        this.key = Options.DEFAULT_DATA_SOURCE.getValue();
        RocketMqDataSourceInfo dataSourceInfo = new RocketMqDataSourceInfo();
        dataSourceInfo.setId("");
        dataSourceInfo.setName("默认数据源");
        dataSourceInfo.setKey("default");
        this.dataSourceInfo = dataSourceInfo;
    }

    /**
     * 构造函数，根据数据源信息创建并初始化 RocketMQ 生产者。
     *
     * @param dataSourceInfo 数据源配置信息，用于构建生产者
     * @throws MQClientException 当生产者启动失败时抛出该异常
     */
    public RocketMqProducer(RocketMqDataSourceInfo dataSourceInfo) throws MQClientException {
        this.dataSourceInfo = dataSourceInfo;
        this.producer = createProducer(dataSourceInfo);
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
    }

    /**
     * 根据数据源信息创建 RocketMQ 的 DefaultMQProducer 实例。
     * 如果未提供访问密钥或安全密钥，则创建无 ACL 鉴权的生产者；否则创建带 ACL 鉴权的生产者。
     * 同时设置 NameServer 地址、生产者组，并启动生产者。
     *
     * @param dataSourceInfo 包含连接所需配置的数据源信息
     * @return 初始化完成的 DefaultMQProducer 实例
     * @throws MQClientException 当生产者启动过程中发生错误时抛出该异常
     */
    @Transient
    public DefaultMQProducer createProducer(RocketMqDataSourceInfo dataSourceInfo) throws MQClientException {
        DefaultMQProducer producer;
        // 判断是否需要启用 ACL 鉴权机制
        String accessKey = dataSourceInfo.getAccessKey();
        accessKey = MagicAPIConfigUtils.getRocketMQConfig(dataSourceInfo.getKey(), "accessKey", accessKey);
        String secretKey = dataSourceInfo.getSecretKey();
        secretKey = MagicAPIConfigUtils.getRocketMQConfig(dataSourceInfo.getKey(), "secretKey", secretKey);
        if (StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey) ) {
            producer = new DefaultMQProducer();
        } else {
            producer = new DefaultMQProducer(
                    new AclClientRPCHook(new SessionCredentials(accessKey,secretKey)));
        }
        String  nameServer = dataSourceInfo.getNameServer();
        nameServer = MagicAPIConfigUtils.getRocketMQConfig(dataSourceInfo.getKey(), "nameServer", nameServer);
        producer.setNamesrvAddr(nameServer);
        String group = dataSourceInfo.getGroup();
        group = MagicAPIConfigUtils.getRocketMQConfig(dataSourceInfo.getKey(), "group", group);
        if (StringUtils.isBlank(group)) {
            group = "magic-api";
        }
        producer.setProducerGroup(group);
        producer.start();
        return producer;
    }

    /**
     * 发送消息到指定的主题（topic）。
     * 支持同步和异步发送模式。若为同步发送且发送状态不是 SEND_OK，则抛出异常。
     * 异常情况会记录日志并重新抛出 MQClientException。
     *
     * @param topic 消息主题
     * @param tags 消息标签，可用于过滤
     * @param keys 消息键，可用于查询
     * @param body 消息体内容（字节数组）
     * @param async 是否采用异步方式发送消息
     * @throws MQClientException 当发送过程出现异常或同步发送结果非成功时抛出该异常
     */
    private void send(String topic, String tags, String keys, byte[] body , boolean async) throws MQClientException {
        Message message = new Message(topic, tags, keys, body);
        try {
            if (async) {
                // 异步发送消息并注册回调处理发送结果
                this.producer.send(message, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.debug("send message success, sendResult=" + sendResult);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        log.error("send message failed, throwable=" + throwable);
                    }
                });
            } else {
                // 同步发送消息并检查发送结果状态
                SendResult sendResult = this.producer.send(message);
                if (sendResult.getSendStatus() != SendStatus.SEND_OK) {
                    throw new MQClientException("send message failed, sendResult=" + sendResult, null);
                }
            }
        } catch (Exception e) {
            log.error("send message failed, topic={}, tag={}", topic, tags, e);
            throw new MQClientException("send message failed", e);
        }
    }


    /**
     * 同步发送实体对象消息
     * @param topic 主题
     * @param msg 发送的消息
     */
    @Comment("同步发送实体对象消息")
    public void send(@Comment(name = "topic", value = "主题") String topic,@Comment(name = "msg", value = "发送的消息") Object msg)  {
        try {
            // 将消息对象转换为JSON字节数组并同步发送
            send(topic, null, null, JsonUtils.toJsonBytes(msg), false);
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步发送实体对象消息
     * @param topic 主题
     * @param tag 标签
     * @param msg 发送的消息
     */
    @Comment("同步发送实体对象消息")
    public void send(@Comment(name = "topic", value = "主题") String topic,@Comment(name = "tag", value = "标签") String tag,@Comment(name = "msg", value = "发送的消息") Object msg)  {
        try {
            // 将消息对象转换为JSON字节数组并同步发送
            send(topic, tag, null, JsonUtils.toJsonBytes(msg), false);
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 异步发送消息
     * @param topic 主题
     * @param msg 发送的消息
     */
    @Comment("异步发送消息")
    public void sendAsync(@Comment(name = "topic", value = "主题") String topic,@Comment(name = "msg", value = "发送的消息") Object msg) {
        try {
            // 将消息对象转换为JSON字节数组并异步发送
            send(topic, null, null, JsonUtils.toJsonBytes(msg), true);
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 异步发送消息
     * @param topic 主题
     * @param tag 标签
     * @param msg 发送的消息
     */
    @Comment("异步发送消息")
    public void sendAsync(@Comment(name = "topic", value = "主题") String topic,@Comment(name = "tag", value = "标签") String tag,@Comment(name = "msg", value = "发送的消息") Object msg) {
        try {
            // 将消息对象转换为JSON字节数组并异步发送
            send(topic, tag, null, JsonUtils.toJsonBytes(msg), true);
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 关闭并释放RocketMQ生产者资源
     */
    @Transient
    public void close() {
        try {
            this.producer.shutdown();
        } catch (Exception e) {
            log.error("close producer error", e);
        }
    }

    /**
     * 获取生产者唯一标识符
     *
     * @return 生产者ID
     */
    @Transient
    public String getId() {
        return this.id;
    }

    /**
     * 获取生产者名称
     *
     * @return 生产者名称
     */
    @Transient
    public String getName() {
        return this.name;
    }

    /**
     * 获取生产者数据源键
     *
     * @return 数据源键
     */
    @Transient
    public String getKey() {
        return this.key;
    }

    /**
     * 获取底层的DefaultMQProducer实例
     *
     * @return DefaultMQProducer实例
     */
    @Transient
    public DefaultMQProducer getProducer() {
        return this.producer;
    }

    /**
     * 获取数据源配置信息
     *
     * @return RocketMQ数据源配置信息
     */
    public RocketMqDataSourceInfo getDataSourceInfo() {
        return this.dataSourceInfo;
    }
}
