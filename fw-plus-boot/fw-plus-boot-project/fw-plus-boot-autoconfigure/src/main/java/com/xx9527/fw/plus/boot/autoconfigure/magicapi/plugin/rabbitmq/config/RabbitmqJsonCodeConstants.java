package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.config;

import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.JsonCode;

public interface RabbitmqJsonCodeConstants extends JsonCodeConstants {

    public static final JsonCode RABBITMQ_DATASOURCE_HOST_REQUIRED = new JsonCode(10001001, "数据源主机地址不能为空");

    public static final JsonCode RABBITMQ_DATASOURCE_PORT_REQUIRED = new JsonCode(10001002, "数据源端口不能为空");

    public static final JsonCode RABBITMQ_DATASOURCE_TYPE_REQUIRED = new JsonCode(10001003, "数据源类型不能为空");

    public static final JsonCode RABBITMQ_DATASOURCE_TYPE_UNDEFINED = new JsonCode(10001004, "数据源类型未定义");

    public static final JsonCode RABBITMQ_DATASOURCE_SERVER_VIRTUALHOST_REPEAT = new JsonCode(10001005, "数据源服务器虚拟主机重复");

    public static final JsonCode RABBITMQ_SUBSCRIBE_KEY_REQUIRED = new JsonCode(10001006, "订阅Key不能为空");

    public static final JsonCode RABBITMQ_SUBSCRIBE_QUEUE_REQUIRED= new JsonCode(10001007, "订阅序列不能为空");

    public static final JsonCode RABBITMQ_SUBSCRIBE_KEY_QUEUE_REPEAT= new JsonCode(10001008, "订阅Key序列重复");


}