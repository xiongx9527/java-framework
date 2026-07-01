package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.config;

import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "magic-api.cluster")
public class ClusterConfig {

	/**
	 * redis 通道
	 */
	@Setter
	private String channel = "magic-api:notify:channel";

	/**
	 * 获取带环境后缀的通道名，避免不同环境共用同一 Redis 时相互串台
	 * 通过获取当前激活的 Spring Profile，将其拼接到通道名后面作为后缀
	 *
	 * @return 包含环境后缀的 Redis 通道名称，如果未激活任何环境则返回原始通道名
	 */
	public String getChannel() {
		String[] profiles = SpringUtils.getActiveProfiles();
		return profiles.length > 0 ? this.channel + ":" + profiles[0] : this.channel;
	}
}
