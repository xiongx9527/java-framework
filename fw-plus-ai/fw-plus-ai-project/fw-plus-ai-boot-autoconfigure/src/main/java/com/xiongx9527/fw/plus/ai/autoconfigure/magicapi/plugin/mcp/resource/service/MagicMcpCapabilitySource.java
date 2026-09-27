package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 能力快照数据源实现：每次取运行态最新值，避免注册表与能力注册器之间的构造期环。
 */
public class MagicMcpCapabilitySource
		implements MagicApiMcpCapabilityRegistrar.CapabilitySource {

	private final ObjectProvider<List<McpServerRuntime>> runtimesProvider;

	private final ObjectProvider<MagicDynamicMcpServer> dynamicServerProvider;

	private final ObjectProvider<MagicMcpResourceMagicDynamicRegistry> resourceRegistryProvider;

	private final ObjectProvider<MagicMcpPromptMagicDynamicRegistry> promptRegistryProvider;

	public MagicMcpCapabilitySource(ObjectProvider<List<McpServerRuntime>> runtimesProvider,
			ObjectProvider<MagicDynamicMcpServer> dynamicServerProvider,
			ObjectProvider<MagicMcpResourceMagicDynamicRegistry> resourceRegistryProvider,
			ObjectProvider<MagicMcpPromptMagicDynamicRegistry> promptRegistryProvider) {
		this.runtimesProvider = runtimesProvider;
		this.dynamicServerProvider = dynamicServerProvider;
		this.resourceRegistryProvider = resourceRegistryProvider;
		this.promptRegistryProvider = promptRegistryProvider;
	}

	@Override
	public List<FeatureMgr> featureMgrs() {
		List<FeatureMgr> featureMgrs = new ArrayList<>();
		List<McpServerRuntime> runtimes = runtimesProvider == null ? null : runtimesProvider.getIfAvailable();
		MagicDynamicMcpServer dynamicServer =
				dynamicServerProvider == null ? null : dynamicServerProvider.getIfAvailable();
		for (McpServerRuntime runtime : runtimes == null
				? Collections.<McpServerRuntime>emptyList() : runtimes) {
			if (runtime.getFeatureMgr() != null) {
				featureMgrs.add(runtime.getFeatureMgr());
			}
		}
		if (dynamicServer != null) {
			for (McpServerRuntime runtime : dynamicServer.serverNodes()) {
				if (runtime.getFeatureMgr() != null) {
					featureMgrs.add(runtime.getFeatureMgr());
				}
			}
		}
		return featureMgrs;
	}

	@Override
	public List<MagicMcpResourceInfo> resources() {
		MagicMcpResourceMagicDynamicRegistry registry =
				resourceRegistryProvider == null ? null : resourceRegistryProvider.getIfAvailable();
		return registry == null ? Collections.<MagicMcpResourceInfo>emptyList() : registry.mappings();
	}

	@Override
	public List<MagicMcpPromptInfo> prompts() {
		MagicMcpPromptMagicDynamicRegistry registry =
				promptRegistryProvider == null ? null : promptRegistryProvider.getIfAvailable();
		return registry == null ? Collections.<MagicMcpPromptInfo>emptyList() : registry.mappings();
	}
}
