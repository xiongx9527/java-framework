/**
 * base 的增强抽象层：在 langchain4j 形状的 AI 组件端口之上，补齐「编排 + 横切 + 边界」三类能力。
 *
 * <p><b>分层</b>：{@code fw-plus-ai-base} 只放 AI 组件端口（模型 / 向量 / 检索 / 消息 / 工具 / 模板 / MCP / 技能），
 * 由各后端的<b>组件子模块</b>实现；本模块放「增强端口」，由各后端的 <b>adapter</b> 实现
 * （adapter 同时负责把组件子模块拼成 base 契约）。依赖方向单向：{@code support -> base}。</p>
 *
 * <p><b>内容</b>：</p>
 * <ul>
 *   <li>{@code workflow}：{@code WorkflowPort} 及其请求 / 结果 / 步骤 / 预算 / 回调 DTO；</li>
 *   <li>（langchain4j 形状的抽象一律放 {@code fw-plus-ai-base}，含 {@code agentic} 包的 {@code AgenticServices}）</li>
 *   <li>{@code context}：{@code CallContext} / {@code PortError} 横切 DTO；</li>
 *   <li>{@code resilience}：模型边界监听、健康与熔断、配额限流、故障转移；</li>
 *   <li>{@code cost} / {@code cache} / {@code idempotency} / {@code guard}：成本核算、缓存与失效纪元、请求幂等、出站地址校验；</li>
 *   <li>{@code io}：文件 / 导出制品与语音（ASR / TTS）边界；</li>
 *   <li>{@code skill}：远程技能目录端口（本地 SKILL.md 装载见 base 的 {@code skill} 包）。</li>
 * </ul>
 *
 * <p>出处：除 {@code agentic}（langchain4j-agentic 的静态 builder 形状）外，其余均为自行抽象（C 类），
 * 对应 {@code docs/ai-layer} 的增强清单与端口层规范。禁止 import 任何具体框架实现类。</p>
 *
 * @author xiongxiao
 */
package com.xiongx9527.fw.plus.ai.support;
