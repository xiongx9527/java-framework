package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

@RefreshScope
@ConfigurationProperties("magic-api.plugin.model")
public class MagicAPIModelConfig extends HashMap<String, Object> {
}
