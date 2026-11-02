/**
 * AgentScope 的 A2A（Agent-to-Agent）跨进程协作实现。
 *
 * <p><b>作用</b>：让本进程 Agent 调用远端 Agent——发现能力、发送任务、接收结果，
 * 对应 AgentScope 的 {@code a2aBuilder()}。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。对应 {@code dev.langchain4j.agentic.a2a}
 * （langchain4j-agentic-a2a 1.22.0-beta32）的模块划分与 AgentScope 的 a2a 能力。</p>
 *
 * @author xiongxiao
 */
package com.xiongx9527.fw.plus.ai.agentscope.a2a;
