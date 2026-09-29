package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class McpCapabilitiesTest {

	@Test
	void shouldMapMethodPrefixToCapability() {
		assertThat(McpCapabilities.ofMethod("tools/list")).isEqualTo("tools");
		assertThat(McpCapabilities.ofMethod("tools/call")).isEqualTo("tools");
		assertThat(McpCapabilities.ofMethod("resources/read")).isEqualTo("resources");
		assertThat(McpCapabilities.ofMethod("prompts/get")).isEqualTo("prompts");
	}

	@Test
	void shouldNotTreatNonCapabilityOperationsAsScoped() {
		assertThat(McpCapabilities.ofMethod("initialize")).isNull();
		assertThat(McpCapabilities.ofMethod("completion/complete")).isNull();
		assertThat(McpCapabilities.ofMethod("ping")).isNull();
		assertThat(McpCapabilities.ofMethod(null)).isNull();
	}

	@Test
	void shouldExtractMethodFromJsonRpcBody() {
		assertThat(McpCapabilities.ofBody("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\"}"))
				.isEqualTo("tools");
		assertThat(McpCapabilities.ofBody("{}")).isNull();
		assertThat(McpCapabilities.ofBody("not json")).isNull();
		assertThat(McpCapabilities.ofBody(null)).isNull();
	}

	@Test
	void shouldCoverWhenScopeMatchesCapabilityNameOrSegment() {
		McpPrincipal principal = principal(Collections.singletonList("tools"));

		assertThat(McpCapabilities.covers(principal, "tools")).isTrue();
		assertThat(McpCapabilities.covers(principal, "resources")).isFalse();

		McpPrincipal prefixed = principal(Collections.singletonList("mcp:prompts"));
		assertThat(McpCapabilities.covers(prefixed, "prompts")).isTrue();
		assertThat(McpCapabilities.covers(prefixed, "tools")).isFalse();
	}

	@Test
	void shouldPassThroughWhenCapabilityUnknownOrScopesAbsent() {
		assertThat(McpCapabilities.covers(principal(Collections.singletonList("tools")), null)).isTrue();
		assertThat(McpCapabilities.covers(McpPrincipal.PERMIT_ALL, "tools")).isTrue();
		assertThat(McpCapabilities.covers(principal(Collections.<String>emptyList()), "tools")).isTrue();
	}

	@Test
	void shouldKeepUnaffectedCapabilitiesWhenOneIsMissing() {
		McpPrincipal principal = principal(Arrays.asList("tools", "resources"));

		assertThat(McpCapabilities.covers(principal, "tools")).isTrue();
		assertThat(McpCapabilities.covers(principal, "resources")).isTrue();
		assertThat(McpCapabilities.covers(principal, "prompts")).isFalse();
	}

	private McpPrincipal principal(Iterable<String> scopes) {
		java.util.Set<String> set = new HashSet<>();
		for (String scope : scopes) {
			set.add(scope);
		}
		return new McpPrincipal("tester", set);
	}
}
