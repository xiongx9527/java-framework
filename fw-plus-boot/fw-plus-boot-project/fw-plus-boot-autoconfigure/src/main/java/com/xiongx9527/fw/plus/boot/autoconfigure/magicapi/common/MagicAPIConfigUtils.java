package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common;

import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * MagicAPI 配置工具类
 * 封装 MagicAPI 相关的配置获取逻辑，避免重复的 String.format 调用
 */
public class MagicAPIConfigUtils {

    private static final String PREFIX = "magic.api.";

    protected static String getConfigInternal(String prefix, String key, String configKey, String defaultValue) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(configKey)) {
            return defaultValue;
        }
        return SpringUtils.getAppConfig(prefix + key + "." + configKey, defaultValue);
    }

    protected static String getConfigInternal(String prefix, String key, String topic, String configKey, String defaultValue) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(topic) || StringUtils.isBlank(configKey)) {
            return defaultValue;
        }
        return SpringUtils.getAppConfig(prefix + key + ".subscribe." + topic + "." + configKey, defaultValue);
    }

    private static String getSimpleConfig(String suffix, String defaultValue) {
        if (StringUtils.isBlank(suffix)) {
            return defaultValue;
        }
        return SpringUtils.getAppConfig(PREFIX + suffix, defaultValue);
    }

    public static String getDbConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "db.", key, configKey, defaultValue);
    }

    public static String getDebeziumConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "debezium.", key, configKey, defaultValue);
    }

    public static String getInfluxConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "influx.", key, configKey, defaultValue);
    }

    public static String getPulsarConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "pulsar.", key, configKey, defaultValue);
    }

    public static String getPulsarSubscribeConfig(String key, String topic, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "pulsar.", key, topic, configKey, defaultValue);
    }

    public static String getKafkaConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "kafka.", key, configKey, defaultValue);
    }

    public static String getKafkaSubscribeConfig(String key, String topic, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "kafka.", key, topic, configKey, defaultValue);
    }

    public static String getMqttConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "mqtt.", key, configKey, defaultValue);
    }

    public static String getElasticSearchConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "elasticsearch.", key, configKey, defaultValue);
    }

    public static String getS3Config(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "s3.", key, configKey, defaultValue);
    }

    public static String getWsConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "ws.", key, configKey, defaultValue);
    }

    public static String getSseConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "sse.", key, configKey, defaultValue);
    }

    public static String getRabbitMQConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "rabbitmq.", key, configKey, defaultValue);
    }

    public static String getRabbitMQSubscribeConfig(String key, String queueName, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "rabbitmq.", key, queueName, configKey, defaultValue);
    }

    public static String getMagicAPIConfig(String configKey, String defaultValue) {
        return getSimpleConfig(configKey, defaultValue);
    }

    public static String getRedisConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "redis.", key, configKey, defaultValue);
    }

    public static String getTaskConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "task.", key, configKey, defaultValue);
    }

    public static String getRocketMQConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "rocketmq.", key, configKey, defaultValue);
    }

    public static String getRocketMQSubscribeConfig(String key, String topic, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "rocketmq.", key, topic, configKey, defaultValue);
    }

    public static String getDtpConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "dtp.", key, configKey, defaultValue);
    }

    public static String getMongoConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "mongo.", key, configKey, defaultValue);
    }

    public static String getMqttClientConfig(String key, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "mqttclient.", key, configKey, defaultValue);
    }

    public static String getMqttClientSubscribeConfig(String key, String topic, String configKey, String defaultValue) {
        return getConfigInternal(PREFIX + "mqttclient.", key, topic, configKey, defaultValue);
    }
}