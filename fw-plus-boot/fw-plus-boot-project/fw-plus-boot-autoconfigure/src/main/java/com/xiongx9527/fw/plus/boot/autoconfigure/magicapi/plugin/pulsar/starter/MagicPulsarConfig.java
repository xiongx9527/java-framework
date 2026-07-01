package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

/**
 * @author zzy
 */
@RefreshScope
@ConfigurationProperties("magic-api.plugin.pulsar")
public class MagicPulsarConfig extends HashMap<String, Object> {
}
