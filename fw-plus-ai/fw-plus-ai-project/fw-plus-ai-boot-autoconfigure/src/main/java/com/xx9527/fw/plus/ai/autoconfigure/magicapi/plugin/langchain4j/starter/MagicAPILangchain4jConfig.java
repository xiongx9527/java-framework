package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

@RefreshScope
@ConfigurationProperties("magic-api.plugin.langchain4j")
public class MagicAPILangchain4jConfig extends HashMap<String, Object> {
}
