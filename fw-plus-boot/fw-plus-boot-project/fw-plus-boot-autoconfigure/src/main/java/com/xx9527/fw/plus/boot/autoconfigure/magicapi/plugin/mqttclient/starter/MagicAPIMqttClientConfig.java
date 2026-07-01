
package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

@RefreshScope
@ConfigurationProperties("magic-api.plugin.mqttclient")
public class MagicAPIMqttClientConfig extends HashMap<String, Object> {
}