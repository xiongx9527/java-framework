package com.xiongx9527.fw.plus.ai.support.resilience;

/**
 * 候选失败时的处置动作（框架提供的策略枚举，业务侧按错误类型配置）。
 *
 * <ul>
 *   <li>{@link #RETRY}：在同一候选上重试（瞬态错误：限流 / 网关 5xx / 超时等，换一个可能就好）；</li>
 *   <li>{@link #FAILOVER}：切换到下一候选模型（该候选不可用，但换一个可能可用）；</li>
 *   <li>{@link #ABORT}：立即终止并向调用方上抛（调用方自身错误，重试 / 切换都无济于事）。</li>
 * </ul>
 */
public enum FailoverAction {

	/** 同一候选重试（耗尽重试次数后自动退化为 {@link #FAILOVER}）。 */
	RETRY,

	/** 切换到下一候选模型。 */
	FAILOVER,

	/** 立即终止并上抛错误。 */
	ABORT
}
