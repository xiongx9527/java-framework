package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class McpPrincipal {

	private String subject;

	private Set<String> scopes = new HashSet<>();

	public McpPrincipal(String subject, Set<String> scopes) {
		this.subject = subject;
		this.scopes = scopes == null ? Collections.emptySet() : scopes;
	}
}
