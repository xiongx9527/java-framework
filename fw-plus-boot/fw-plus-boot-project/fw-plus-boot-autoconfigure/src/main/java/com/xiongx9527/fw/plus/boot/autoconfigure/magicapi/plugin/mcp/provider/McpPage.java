package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpPage<T> {

	private List<T> items;

	private String nextCursor;

	public static <T> McpPage<T> of(List<T> items, String nextCursor) {
		return new McpPage<>(items, nextCursor);
	}
}
