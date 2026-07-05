package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "magic-api.plugin.mcp")
public class MagicAPIMcpProperties {

	private boolean enabled = true;

	private String endpoint = "/mcp";

	private long sessionTimeoutSeconds = 1800;

	private boolean sseStreamEnabled = true;

	private int maxRequestBytes = 1024 * 1024;

	private int maxResponseChars = 1024 * 1024;

	private List<String> trustedOrigins = new ArrayList<>();

	private Auth auth = new Auth();

	@Data
	public static class Auth {

		private boolean enabled = true;

		private String resource = "fw-plus-mcp";

		private String authorizationServer;

		private String toolsScope = "mcp:tools";

		private String resourcesScope = "mcp:resources";

		private String promptsScope = "mcp:prompts";

		private String loggingScope = "mcp:logging";

		private String rootsScope = "mcp:roots";

		private String samplingScope = "mcp:sampling";

		private String elicitationScope = "mcp:elicitation";
	}
}
