/**
 * 编排契约：把「目标 + 步骤 + 预算」交给实现去跑（AgentScope / langchain4j-agentic 落地）。
 *
 * <p>只暴露 goal / step / budget，不把 Agent、Scope、Supervisor 等拓扑概念写进端口——
 * 换实现不用改契约。</p>
 */
package com.xiongx9527.fw.plus.ai.support.workflow;
