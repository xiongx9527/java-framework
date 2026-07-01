package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;

@ConfigurationProperties("magic-api.plugin.rabbitmq")
public class MagicRabbitMqConfig  extends HashMap<String, Object> {
}
