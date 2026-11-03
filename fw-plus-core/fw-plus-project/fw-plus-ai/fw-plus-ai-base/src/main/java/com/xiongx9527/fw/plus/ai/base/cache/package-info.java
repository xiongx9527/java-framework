/**
 * 缓存端口：带「纪元」的失效机制。
 *
 * <p>数据变更时只递增 epoch，所有带旧 epoch 的键自然失效——不需要遍历删除。</p>
 */
package com.xiongx9527.fw.plus.ai.base.cache;
