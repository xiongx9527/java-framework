package com.xiongx9527.fw.plus.ai.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON-RPC 模型与错误码单元测试
 *
 * <p>覆盖 spec 4.1/4.2：JSON-RPC 2.0 序列化、错误码（-32700/-32600/-32601/-32602/-32603）、
 * 工具错误结果与协议错误的区分。
 */
public class McpJsonRpcTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void shouldSerializeResponseWithResult() throws Exception {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("protocolVersion", "2025-11-25");
		String json = mapper.writeValueAsString(McpJsonRpcResponse.success("abc", result));
		assertThat(json).contains("\"jsonrpc\":\"2.0\"");
		assertThat(json).contains("\"id\":\"abc\"");
		assertThat(json).contains("\"protocolVersion\":\"2025-11-25\"");
		assertThat(json).doesNotContain("\"error\"");
	}

	@Test
	public void shouldSerializeResponseWithError() throws Exception {
		String json = mapper.writeValueAsString(
				McpJsonRpcResponse.error(42, McpJsonRpcError.methodNotFound("foo")));
		assertThat(json).contains("\"id\":42");
		assertThat(json).contains("\"error\"");
		assertThat(json).contains("\"code\":-32601");
		assertThat(json).contains("\"message\":\"Method not found: foo\"");
	}

	@Test
	public void shouldNotIncludeNullFieldsInError() throws Exception {
		String json = mapper.writeValueAsString(
				McpJsonRpcResponse.error(1, new McpJsonRpcError(-32602, "bad params")));
		assertThat(json).doesNotContain("\"data\"");
	}

	@Test
	public void shouldParseRequestWithIdAndParams() throws Exception {
		String json = "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\","
				+ "\"params\":{\"name\":\"foo\",\"arguments\":{\"x\":1}}}";
		McpJsonRpcRequest request = mapper.readValue(json, McpJsonRpcRequest.class);
		assertThat(request.getId()).isEqualTo(7);
		assertThat(request.getMethod()).isEqualTo("tools/call");
		assertThat(request.getParams()).containsEntry("name", "foo");
	}

	@Test
	public void shouldIgnoreUnknownFieldsWhenParsing() throws Exception {
		String json = "{\"jsonrpc\":\"2.0\",\"id\":\"r1\",\"method\":\"ping\",\"unknown\":42}";
		McpJsonRpcRequest request = mapper.readValue(json, McpJsonRpcRequest.class);
		assertThat(request.getMethod()).isEqualTo("ping");
	}

	@Test
	public void shouldExposeStandardErrorCodes() {
		assertThat(McpJsonRpcError.PARSE_ERROR).isEqualTo(-32700);
		assertThat(McpJsonRpcError.INVALID_REQUEST).isEqualTo(-32600);
		assertThat(McpJsonRpcError.METHOD_NOT_FOUND).isEqualTo(-32601);
		assertThat(McpJsonRpcError.INVALID_PARAMS).isEqualTo(-32602);
		assertThat(McpJsonRpcError.INTERNAL_ERROR).isEqualTo(-32603);
	}
}
