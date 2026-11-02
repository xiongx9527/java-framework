/**
 * native 组件：Azure Cosmos DB NoSQL。
 *
 * <p>属于 （向量 / 图）存储 组，只做协议与传输/编排，由 adapter 组装成 base / support 的端口实现。
 *
 * <p><b>出处</b>：C 类（自行抽象）——native 后端不走 langchain4j / Spring AI，直连厂商 API；
 * 端口形状以 {@code fw-plus-ai-base} 与 {@code fw-plus-ai-support} 的契约为准。
 *
 * @author xiongxiao
 */
package com.xiongx9527.fw.plus.ai.native.store.azurecosmosnosql;
