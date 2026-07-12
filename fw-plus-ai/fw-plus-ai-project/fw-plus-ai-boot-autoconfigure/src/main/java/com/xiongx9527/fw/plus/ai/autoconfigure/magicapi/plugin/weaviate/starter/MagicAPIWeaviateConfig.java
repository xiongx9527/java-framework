package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

@RefreshScope
@ConfigurationProperties("magic-api.plugin.weaviate")
public class MagicAPIWeaviateConfig extends HashMap<String, Object> {
}
