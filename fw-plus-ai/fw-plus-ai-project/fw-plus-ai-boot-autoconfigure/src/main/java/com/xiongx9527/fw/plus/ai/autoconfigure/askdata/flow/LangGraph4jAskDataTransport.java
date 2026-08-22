package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentGraph;
import com.xiongx9527.fw.plus.ai.askdata.agent.AskDataAgentNodeExecutor;

import java.util.List;

/**
 * 由业务实现的 Java LangGraph/LangGraph4j 传输边界。
 *
 * <p>Java 8 Starter 不强制引入某个 LangGraph 发行版。业务 Adapter 负责版本兼容、状态图创建和调用，节点实际业务
 * 执行仍回调 Core {@link AskDataAgentNodeExecutor}，从而保持权限、状态和审计边界。</p>
 */
public interface LangGraph4jAskDataTransport {
	boolean isAvailable();
	List<String> execute(AskDataAgentGraph graph, AskDataAgentNodeExecutor executor);
}
