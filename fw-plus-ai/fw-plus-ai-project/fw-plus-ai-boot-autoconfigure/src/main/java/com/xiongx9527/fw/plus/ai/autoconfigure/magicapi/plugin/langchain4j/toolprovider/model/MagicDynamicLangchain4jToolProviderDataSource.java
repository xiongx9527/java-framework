package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module.Langchain4jAgenticScriptModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticToolProviders;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.Langchain4jToolProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicApiToolExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicApiToolProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicApiToolRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.service.tool.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code langchain4j-tool-provider} 数据源的实例持有者：按 {@code key} 持有并驱逐多个
 * {@link MagicApiToolProvider} 实例，每个实例由各自数据源定义的工具范围独立构建。
 *
 * <p>同时持有每实例的 {@link MagicApiToolRegistry}，用于在 api/function 资源变更后
 * 统一失效全部实例的工具快照（{@link #invalidateAll()}）。</p>
 */
public class MagicDynamicLangchain4jToolProviderDataSource implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(MagicDynamicLangchain4jToolProviderDataSource.class);

	private final MagicResourceService magicResourceService;

	private final MagicToolInvoker invoker;

	private final Map<String, ToolProvider> providers = new ConcurrentHashMap<>();

	private final Map<String, MagicApiToolRegistry> registries = new ConcurrentHashMap<>();

	private final AgenticPipelineRegistry pipelineRegistry;

	private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module.Langchain4jAgenticScriptModule agenticScriptModule;

	public MagicDynamicLangchain4jToolProviderDataSource(MagicResourceService magicResourceService,
			MagicToolInvoker invoker) {
		this(magicResourceService, invoker, null, null);
	}

	public MagicDynamicLangchain4jToolProviderDataSource(MagicResourceService magicResourceService,
			MagicToolInvoker invoker, AgenticPipelineRegistry pipelineRegistry,
			com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module.Langchain4jAgenticScriptModule agenticScriptModule) {
		this.magicResourceService = magicResourceService;
		this.invoker = invoker;
		this.pipelineRegistry = pipelineRegistry;
		this.agenticScriptModule = agenticScriptModule;
	}

	/**
	 * 按资源定义创建（或重建）一个 ToolProvider 实例。
	 *
	 * @param info 资源定义
	 * @return 创建成功返回 {@code true}
	 */
	public boolean create(Langchain4jToolProviderInfo info) {
		String key = info.getKey();
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		try {
			destroy(key);
			if (info.getAgenticKey() != null && !info.getAgenticKey().trim().isEmpty()) {
				// agentic 来源：工具即一个编排（agent-as-tool，包装收口在隔离层）。
				// agenticKey 两种绑定：① 编排注册中心的 key（如 nl2sql 流水线）；② agentic-script 脚本资源路径（回落）
				UntypedAgent pipeline = pipelineRegistry == null ? null : pipelineRegistry.get(info.getAgenticKey().trim());
				if (pipeline == null && agenticScriptModule != null) {
					pipeline = agenticScriptModule.node(info.getAgenticKey().trim());
				}
				if (pipeline == null) {
					throw new IllegalArgumentException(String.format(
							"agenticKey '%s' 未命中：编排注册中心与 agentic-script 脚本资源中均不存在", info.getAgenticKey()));
				}
				providers.put(key, AgenticToolProviders.agenticToolProvider(pipeline, key,
						info.getAgenticDescription() != null && !info.getAgenticDescription().trim().isEmpty()
								? info.getAgenticDescription() : info.getName(),
						info.getAgenticOutputKey(),
						info.getAgenticInputKeys() == null ? new String[0]
								: info.getAgenticInputKeys().toArray(new String[0])));
				logger.info("注册 LangChain4j ToolProvider(agentic): {} -> {}", key, info.getAgenticKey());
				return true;
			}
			Langchain4jToolProperties.Tools scope = info.getTools() == null
					? new Langchain4jToolProperties.Tools() : info.getTools();
			MagicApiToolRegistry registry = new MagicApiToolRegistry(magicResourceService, scope);
			MagicApiToolExecutor executor = new MagicApiToolExecutor(registry, invoker);
			MagicApiToolProvider provider = new MagicApiToolProvider(registry, executor);
			registries.put(key, registry);
			providers.put(key, provider);
			logger.info("注册 LangChain4j ToolProvider: {}", key);
			return true;
		} catch (Throwable e) {
			logger.warn("构建 LangChain4j ToolProvider 失败: key={}", key, e);
			return false;
		}
	}

	/**
	 * 按 key 销毁实例（连同其注册表）。
	 */
	public void destroy(String key) {
		if (key == null) {
			return;
		}
		registries.remove(key);
		providers.remove(key);
	}

	/**
	 * 按 key 取 ToolProvider，不存在时返回 {@code null}。
	 */
	public ToolProvider get(String key) {
		return key == null ? null : providers.get(key);
	}

	/**
	 * 当前已配置的 key 集合（不可修改视图）。
	 */
	public Set<String> keys() {
		return Collections.unmodifiableSet(providers.keySet());
	}

	/**
	 * api/function 资源变更后调用：使全部实例的工具快照失效，下次提供时重建。
	 */
	public void invalidateAll() {
		for (MagicApiToolRegistry registry : registries.values()) {
			registry.invalidateSnapshot();
		}
	}

	@Override
	public void destroy() {
		registries.clear();
		providers.clear();
	}
}
