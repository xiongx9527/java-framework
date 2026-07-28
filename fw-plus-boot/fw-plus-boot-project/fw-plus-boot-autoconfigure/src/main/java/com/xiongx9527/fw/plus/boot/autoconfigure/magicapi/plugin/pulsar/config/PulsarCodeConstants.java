package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.config;

import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.JsonCode;

public interface PulsarCodeConstants extends JsonCodeConstants {

    public static final JsonCode PULSAR_DATASOURCE_SERVERS_REQUIRED = new JsonCode(40001001, "数据源服务器不能为空");

    public static final JsonCode PULSAR_DATASOURCE_TENANT_REQUIRED = new JsonCode(40001002, "数据源租户不能为空");

    public static final JsonCode PULSAR_DATASOURCE_NAMESPACES_REQUIRED = new JsonCode(40001003, "数据源命名不能为空");

    public static final JsonCode PULSAR_DATASOURCE_OTHER_MUST_BE_JSONOBJECT = new JsonCode(40001004, "数据源其它配置必须为JSONObject");

    public static final JsonCode PULSAR_SUBSCRIBE_KEY_REQUIRED = new JsonCode(40001005, "订阅key不能为空");

    public static final JsonCode PULSAR_SUBSCRIBE_TOPIC_REQUIRED = new JsonCode(40001006, "订阅主题不能为空");

    public static final JsonCode PULSAR_SUBSCRIBE_KEY_TOPIC_REPEAT= new JsonCode(40001007, "订阅key主题重复");

    public static final JsonCode PULSAR_SUBSCRIBE_GROUP_REQUIRED = new JsonCode(40001008, "订阅组不能为空");

    public static final JsonCode PULSAR_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT = new JsonCode(40001009, "订阅其它配置必须为JSONObject");

    public static final JsonCode PULSAR_DATASOURCE_REFERENCED_BY_SUBSCRIBE = new JsonCode(40001010, "数据源被订阅配置引用，无法删除");

    public static final JsonCode PULSAR_SUBSCRIBE_ENABLED_CANNOT_DELETE = new JsonCode(40001011, "订阅已启用，请先停用后再删除");

    public static final JsonCode PULSAR_DATASOURCE_CONNECT_FAILED = new JsonCode(40001012, "数据源连接失败");

}