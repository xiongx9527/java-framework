package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;

/**
 * @author zzy
 */
@ConfigurationProperties("magic-api.plugin.rocketmq")
public class MagicRocketMqConfig  extends HashMap<String, Object> {
}
