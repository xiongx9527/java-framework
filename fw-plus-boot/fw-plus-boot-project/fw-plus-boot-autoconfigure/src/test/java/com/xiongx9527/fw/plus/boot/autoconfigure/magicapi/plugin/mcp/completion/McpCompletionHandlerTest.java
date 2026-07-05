package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.completion;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class McpCompletionHandlerTest {

	@Test
	void shouldCompletePromptArgument() {
		McpCompletionHandler handler = new McpCompletionHandler(Collections.singletonList(new FixedCompletionProvider()));
		Map<String, Object> ref = new HashMap<>();
		ref.put("type", "ref/prompt");
		ref.put("name", "greet");
		Map<String, Object> argument = new HashMap<>();
		argument.put("name", "name");
		argument.put("value", "9");
		Map<String, Object> params = new HashMap<>();
		params.put("ref", ref);
		params.put("argument", argument);

		Map<String, Object> result = handler.complete(new McpRequestContext(), params);

		assertThat(result.get("completion")).isInstanceOf(Map.class);
		Map<?, ?> completion = (Map<?, ?>) result.get("completion");
		assertThat(completion.get("hasMore")).isEqualTo(false);
	}

	private static class FixedCompletionProvider implements McpCompletionProvider {

		@Override
		public McpCompletionResult complete(McpProviderContext context, McpCompletionRequest request) {
			McpCompletionResult result = new McpCompletionResult();
			result.setValues(Collections.singletonList("9527"));
			result.setHasMore(false);
			return result;
		}
	}
}
