package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.Data;

@Data
public class McpPageRequest {

	private String cursor;

	private int limit = 100;
}
