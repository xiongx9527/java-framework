package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.config;

import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.JsonCode;

public interface RocketmqJsonCodeConstants extends JsonCodeConstants {

	public static final JsonCode ROCKETMQ_DATASOURCE_NAMESERVER_REQUIRED = new JsonCode(20001001, "数据源名称服务器不能为空");

	public static final JsonCode ROCKETMQ_DATASOURCE_TYPE_REQUIRED = new JsonCode(20001002, "数据源类型不能为空");

	public static final JsonCode ROCKETMQ_DATASOURCE_TYPE_UNDEFINED = new JsonCode(20001003, "数据源类型未定义");

	public static final JsonCode ROCKETMQ_DATASOURCE_SERVER_GROUP_REPEAT = new JsonCode(20001004, "数据源服务器组重复");

	public static final JsonCode ROCKETMQ_SUBSCRIBE_KEY_REQUIRED  = new JsonCode(20001005, "订阅key不能为空");

	public static final JsonCode ROCKETMQ_SUBSCRIBE_TOPIC_REQUIRED  = new JsonCode(20001006, "订阅主题不能为空");

	public static final JsonCode ROCKETMQ_SUBSCRIBE_TAG_REQUIRED  = new JsonCode(20001007, "订阅标签不能为空");

	public static final JsonCode ROCKETMQ_SUBSCRIBE_KEY_TOPIC_REPEAT  = new JsonCode(20001008, "订阅key主题重复");

}
