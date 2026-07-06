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

	private Headers headers = new Headers();

	private Protocol protocol = new Protocol();

	private Tools tools = new Tools();

	@Data
	public static class Protocol {

		private String defaultVersion = "2025-11-25";

		private List<String> supportedVersions = new ArrayList<String>() {{
			add("2025-11-25");
			add("2025-06-18");
		}};
	}

	@Data
	public static class Headers {

		private String accept = "Accept";

		private String protocolVersion = "MCP-Protocol-Version";

		private String sessionId = "Mcp-Session-Id";

		private String origin = "Origin";
	}

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

	@Data
	public static class Tools {

		private Scope functions = new Scope();

		private Api api = new Api();
	}

	@Data
	public static class Scope {

		private boolean enabled = true;

		private List<String> includeGroups = new ArrayList<>();

		private List<String> includePaths = new ArrayList<>();

		private List<String> excludePaths = new ArrayList<>();
	}

	@Data
	public static class Api extends Scope {

		private List<String> methods = new ArrayList<>();

		private boolean requireOptIn = false;

		private String optInOptionName = "mcpTool";

		public Api() {
			setEnabled(false);
		}
	}
}
