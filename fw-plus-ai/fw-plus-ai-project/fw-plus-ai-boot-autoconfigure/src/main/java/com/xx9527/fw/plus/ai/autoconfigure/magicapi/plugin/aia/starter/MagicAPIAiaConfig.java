package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;
@RefreshScope
@ConfigurationProperties("magic-api.plugin.aia")
public class MagicAPIAiaConfig extends HashMap<String, Object> {
}
