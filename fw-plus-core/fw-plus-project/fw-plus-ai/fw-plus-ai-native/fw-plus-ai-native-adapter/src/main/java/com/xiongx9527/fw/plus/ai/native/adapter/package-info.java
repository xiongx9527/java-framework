/**
 * native 组件：契约实现。
 *
 * <p>用 core 与各组件子模块把 base / support 的端口真正实现出来、做 bean 注入，并落地领域能力（SqlGenerationCapability）；装配由 -boot starter 负责。
 *
 * <p><b>出处</b>：C 类（自行抽象）——native 后端不走 langchain4j / Spring AI，直连厂商 API；
 * 端口形状以 {@code fw-plus-ai-base} 与 {@code fw-plus-ai-support} 的契约为准。
 *
 * @author xiongxiao
 */
package com.xiongx9527.fw.plus.ai.native.adapter;
