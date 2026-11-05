/**
 * 存储契约集合。
 *
 * <ul>
 *   <li>{@link com.xiongx9527.fw.plus.ai.base.store.Store}：所有后端共享的身份、
 *       可用性与生命周期，用于 {@link com.xiongx9527.fw.plus.ai.base.store.StoreRegistry} 统一登记。</li>
 *   <li>{@link com.xiongx9527.fw.plus.ai.base.store.RecordStore}：按 id 的 CRUD 与文本召回，
 *       由图库等支持随机读的后端实现。</li>
 *   <li>{@code vector}：向量存储（写入、删除、维度管理与最近邻检索）。</li>
 *   <li>{@code graph}：图存储（GraphRAG 基础契约：邻居、节点与文本召回）。</li>
 * </ul>
 *
 * <p>向量库不实现 {@code RecordStore}：随机读与「是否存在」不是它的能力，
 * 其读写 / 检索语义由 {@code VectorStorePort} 自己声明，避免按 {@code Store} 多态取用时出现
 * 与契约不符的降级行为。</p>
 */
package com.xiongx9527.fw.plus.ai.base.store;
