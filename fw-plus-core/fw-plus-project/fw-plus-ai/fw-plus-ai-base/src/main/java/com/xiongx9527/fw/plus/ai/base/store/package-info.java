/**
 * 存储契约集合。
 *
 * <ul>
 *   <li>{@code vector}：向量存储（写入、删除、维度管理与最近邻检索）。</li>
 *   <li>{@code graph}：图存储（GraphRAG 基础契约：邻居、节点与文本召回）。</li>
 * </ul>
 *
 * <p>两者都是与具体后端无关的基础契约，由 adapter 用 Neo4j / Qdrant / pgvector 等实现。</p>
 */
package com.xiongx9527.fw.plus.ai.base.store;
