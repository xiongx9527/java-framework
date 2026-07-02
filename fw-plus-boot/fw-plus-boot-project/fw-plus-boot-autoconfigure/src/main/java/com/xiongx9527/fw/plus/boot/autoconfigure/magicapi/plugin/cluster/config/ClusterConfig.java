package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "magic-api.cluster")
public class ClusterConfig {

	/**
	 * redis 通道
	 */
	private String channel = "magic-api:notify:channel";
}
