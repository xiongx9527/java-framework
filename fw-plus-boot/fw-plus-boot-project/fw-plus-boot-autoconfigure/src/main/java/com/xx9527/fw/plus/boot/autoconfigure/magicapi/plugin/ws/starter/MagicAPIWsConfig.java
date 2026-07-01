package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

/**
 * @author zzy
 */
@RefreshScope
@ConfigurationProperties("magic-api.plugin.ws")
public class MagicAPIWsConfig extends HashMap<String, Object> {
}
