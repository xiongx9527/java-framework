package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.config;

import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.JsonCode;

public interface KafkaCodeConstants extends JsonCodeConstants {

    /**
     * Kafka数据源服务器地址不能为空错误码
     * 错误码：30001001
     * 错误信息：数据源服务器不能为空
     */
    public final JsonCode KAFKA_DATASOURCE_SERVERS_REQUIRED = new JsonCode(30001001, "数据源服务器不能为空");

    /**
     * Kafka数据源其他配置必须为JSONObject错误码
     * 错误码：30001004
     * 错误信息：数据源其它配置必须为JSONObject
     */
    public final JsonCode KAFKA_DATASOURCE_OTHER_MUST_BE_JSONOBJECT = new JsonCode(30001004, "数据源其它配置必须为JSONObject");

    /**
     * Kafka订阅key不能为空错误码
     * 错误码：30001005
     * 错误信息：订阅key不能为空
     */
    public static final JsonCode KAFKA_SUBSCRIBE_KEY_REQUIRED = new JsonCode(30001005, "订阅key不能为空");

    /**
     * Kafka订阅主题不能为空错误码
     * 错误码：30001006
     * 错误信息：订阅主题不能为空
     */
    public final JsonCode KAFKA_SUBSCRIBE_TOPIC_REQUIRED = new JsonCode(30001006, "订阅主题不能为空");

    /**
     * Kafka订阅key主题重复错误码
     * 错误码：30001007
     * 错误信息：订阅key主题重复
     */
    public final JsonCode KAFKA_SUBSCRIBE_KEY_TOPIC_REPEAT= new JsonCode(30001007, "订阅key主题重复");

    /**
     * Kafka订阅其他配置必须为JSONObject错误码
     * 错误码：30001008
     * 错误信息：订阅其它配置必须为JSONObject
     */
    public final JsonCode KAFKA_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT = new JsonCode(30001008, "订阅其它配置必须为JSONObject");


}
