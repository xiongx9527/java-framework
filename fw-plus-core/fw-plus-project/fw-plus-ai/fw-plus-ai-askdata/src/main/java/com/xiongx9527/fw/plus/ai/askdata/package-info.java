/**
 * fw-plus-ai-askdata — SQL 生成领域封装（DomainPack）。
 *
 * <p>参考 docs/ai-layer §6.38：把“生成 SQL”提为一等、通用、可独立调用的能力
 * {@code SqlGenerationCapability}（NL2SQL 纯函数 + 显式输入），供内联问数 pipeline
 * 与工具调用（MCP / function calling）共用。</p>
 *
 * <p>设计约束：
 * <ul>
 *   <li>后端无关：只依赖本模块自定义的中立 {@code LlmChatPort}，不依赖任何具体 LLM
 *       框架（口座由各后端 adapter 落地）；</li>
 *   <li>纯函数：给定相同输入结果可复现，不执行、不画图、不回答；</li>
 *   <li>消责方言：只接收上游注入的可见 schema 与约束，不在生成里再判权限。</li>
 * </ul>
 */
package com.xiongx9527.fw.plus.ai.askdata;
