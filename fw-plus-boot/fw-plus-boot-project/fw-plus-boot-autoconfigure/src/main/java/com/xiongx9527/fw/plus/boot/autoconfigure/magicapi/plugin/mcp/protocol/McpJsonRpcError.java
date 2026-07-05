package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpJsonRpcError {

	public static final int PARSE_ERROR = -32700;

	public static final int INVALID_REQUEST = -32600;

	public static final int METHOD_NOT_FOUND = -32601;

	public static final int INVALID_PARAMS = -32602;

	public static final int INTERNAL_ERROR = -32603;

	private int code;

	private String message;

	private Object data;
}
