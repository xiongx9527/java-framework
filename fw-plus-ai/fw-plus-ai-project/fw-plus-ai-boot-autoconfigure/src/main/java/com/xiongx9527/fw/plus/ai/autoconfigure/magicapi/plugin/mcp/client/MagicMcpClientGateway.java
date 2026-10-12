package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.client;

import com.ajaxjs.mcp.protocol.client.ElicitRequestParams;
import com.ajaxjs.mcp.protocol.client.ElicitResult;
import com.ajaxjs.mcp.protocol.client.Root;
import com.ajaxjs.mcp.protocol.client.SamplingCreateMessageParams;
import com.ajaxjs.mcp.protocol.client.SamplingCreateMessageResult;
import com.ajaxjs.mcp.protocol.initialize.InitializeRequestParams.Capabilities;
import com.ajaxjs.mcp.server.McpServer;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 客户端请求网关：在调用内核 {@code listRoots/createMessage/elicit} 前先做 capability 检查，
 * 并暴露 pending 请求状态。
 *
 * <p>内核按 sessionId 索引客户端能力，客户端未在 initialize 里声明对应 capability 时不发起
 * 请求（MCP 规范要求：未声明 capabilities 的请求必须被视为协议错误），直接返回空结果/抛异常。
 * pending 状态由内核 {@link McpServer#getRunningRequests()} 与
 * {@link McpServer#getPendingClientResponses()} 维护，本类只做只读视图，不重复记账。</p>
 */
public class MagicMcpClientGateway {

	/**
	 * 客户端未声明 roots capability 时的缺省返回值：空列表而非 null，调用方无需判空。
	 */
	public static final List<Root> NO_ROOTS = Collections.emptyList();

	private final McpServer server;

	private final Duration defaultTimeout;

	public MagicMcpClientGateway(McpServer server) {
		this(server, Duration.ofSeconds(30));
	}

	public MagicMcpClientGateway(McpServer server, Duration defaultTimeout) {
		this.server = server;
		this.defaultTimeout = defaultTimeout == null ? Duration.ofSeconds(30) : defaultTimeout;
	}

	/**
	 * 客户端是否声明了指定能力。
	 *
	 * @param sessionId 会话 id
	 * @param capability roots / sampling / elicitation
	 * @return 已声明返回 {@code true}
	 */
	public boolean hasCapability(String sessionId, String capability) {
		Capabilities capabilities = capabilitiesOf(sessionId);
		if (capabilities == null) {
			return false;
		}
		if ("roots".equals(capability)) {
			return capabilities.getRoots() != null;
		}
		if ("sampling".equals(capability)) {
			return capabilities.getSampling() != null;
		}
		if ("elicitation".equals(capability)) {
			return capabilities.getElicitation() != null;
		}
		return false;
	}

	/**
	 * 拉取客户端根目录列表。
	 *
	 * @param sessionId 会话 id
	 * @return roots 列表；客户端未声明 roots capability 时为空列表
	 */
	public List<Root> listRoots(String sessionId) {
		if (!hasCapability(sessionId, "roots")) {
			return NO_ROOTS;
		}
		return server.listRoots(sessionId, defaultTimeout);
	}

	/**
	 * 请求客户端执行 sampling。
	 *
	 * @param sessionId 会话 id
	 * @param params    请求参数
	 * @return sampling 结果
	 * @throws IllegalStateException 客户端未声明 sampling capability
	 */
	public SamplingCreateMessageResult createMessage(String sessionId, SamplingCreateMessageParams params) {
		requireCapability(sessionId, "sampling");
		return server.createMessage(sessionId, params, defaultTimeout);
	}

	/**
	 * 请求客户端补充输入（elicitation）。
	 *
	 * @param sessionId 会话 id
	 * @param params    请求参数
	 * @return elicitation 结果
	 * @throws IllegalStateException 客户端未声明 elicitation capability
	 */
	public ElicitResult elicit(String sessionId, ElicitRequestParams params) {
		requireCapability(sessionId, "elicitation");
		return server.elicit(sessionId, params, defaultTimeout);
	}

	/**
	 * 正在等待客户端响应的请求快照：键为内核的 RequestKey（sessionId + 请求 id），
	 * 值为内核的 RunningRequest。签名用通配符，避免把内核内部类型带到公共 API 上。
	 *
	 * @return 进行中的请求
	 */
	public Map<?, ?> runningRequests() {
		return Collections.unmodifiableMap(server.getRunningRequests());
	}

	/**
	 * 已发出但尚未收到响应的 pending 请求（按请求 id 索引到 CompletableFuture）；
	 * 客户端响应到达即 resolved，异常即 failed，由内核维护。
	 *
	 * @return pending 响应期程
	 */
	public Map<String, ?> pendingClientResponses() {
		return Collections.unmodifiableMap(server.getPendingClientResponses());
	}

	/**
	 * 全部已初始化会话的能力视图。
	 *
	 * @return sessionId → capabilities
	 */
	public Map<String, Capabilities> clientCapabilities() {
		return Collections.unmodifiableMap(server.getClientCapabilities());
	}

	private Capabilities capabilitiesOf(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		return server.getClientCapabilities().get(sessionId);
	}

	private void requireCapability(String sessionId, String capability) {
		if (!hasCapability(sessionId, capability)) {
			throw new IllegalStateException(
					"client '" + sessionId + "' did not declare the '" + capability + "' capability");
		}
	}
}
