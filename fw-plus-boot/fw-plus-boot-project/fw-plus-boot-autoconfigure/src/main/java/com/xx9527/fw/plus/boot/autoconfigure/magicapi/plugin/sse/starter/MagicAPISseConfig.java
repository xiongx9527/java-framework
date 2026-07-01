package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

/**
 * @author zzy
 */
@RefreshScope
@ConfigurationProperties("magic-api.plugin.sse")
public class MagicAPISseConfig extends HashMap<String, Object> {
}
