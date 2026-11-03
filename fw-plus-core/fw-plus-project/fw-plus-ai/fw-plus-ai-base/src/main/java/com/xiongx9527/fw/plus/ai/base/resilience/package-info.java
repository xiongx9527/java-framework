/**
 * 韧性：模型边界监听、健康度、配额与故障转移。
 *
 * <p>与 base 的「单次调用重试」互补：这里管的是跨调用的健康度与候选链，
 * 属于产品策略，由 adapter 实现。</p>
 */
package com.xiongx9527.fw.plus.ai.base.resilience;
