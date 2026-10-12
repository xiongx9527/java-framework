package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.mcp;

import com.ajaxjs.mcp.client.IMcpClient;
import com.ajaxjs.mcp.client.McpToolProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 工具聚合器工厂：单/多客户端装配与失败策略透传。
 */
class AjMcpToolProviderFactoryTest {

	@Test
	void shouldWrapSingleClientAsSingletonList() {
		IMcpClient client = mock(IMcpClient.class);

		McpToolProvider provider = AjMcpToolProviderFactory.fromClient(client);

		assertThat(provider.getMcpClients()).containsExactly(client);
		assertThat(provider.isFailIfOneServerFails()).isFalse();
	}

	@Test
	void shouldKeepClientDeclarationOrder() {
		IMcpClient first = mock(IMcpClient.class);
		IMcpClient second = mock(IMcpClient.class);
		List<IMcpClient> clients = Arrays.asList(first, second);

		McpToolProvider provider = AjMcpToolProviderFactory.fromClients(clients);

		assertThat(provider.getMcpClients()).containsExactly(first, second);
	}

	@Test
	void shouldPassThroughFailIfOneServerFails() {
		List<IMcpClient> clients = Arrays.asList(mock(IMcpClient.class));

		McpToolProvider provider = AjMcpToolProviderFactory.fromClients(clients, true);

		assertThat(provider.isFailIfOneServerFails()).isTrue();
	}

	@Test
	void shouldTreatNullClientsAsEmpty() {
		McpToolProvider provider = AjMcpToolProviderFactory.fromClients(null);

		assertThat(provider.getMcpClients()).isEmpty();
	}
}
