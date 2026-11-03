/**
 * 向量存储契约：写入、删除、维度管理与最近邻检索。
 *
 * <p>检索部分只描述「按向量取 topK」，具体后端（Qdrant / Milvus / pgvector / ES）由 adapter 实现。</p>
 */
package com.xiongx9527.fw.plus.ai.base.store;
