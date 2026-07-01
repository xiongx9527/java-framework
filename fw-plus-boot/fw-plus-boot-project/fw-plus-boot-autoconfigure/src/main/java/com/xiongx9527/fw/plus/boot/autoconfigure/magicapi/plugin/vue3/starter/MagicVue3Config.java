package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

@RefreshScope
@ConfigurationProperties("magic-api.plugin.vue3")
public class MagicVue3Config extends HashMap<String, Object> {
}
