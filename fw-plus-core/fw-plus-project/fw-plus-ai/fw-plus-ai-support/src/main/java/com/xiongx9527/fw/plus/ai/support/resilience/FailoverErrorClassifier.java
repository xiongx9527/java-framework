package com.xiongx9527.fw.plus.ai.support.resilience;

/**
 * 故障转移错误分类策略：判断某次候选失败该如何处置（重试 / 切换 / 终止）。
 *
 * <p><b>框架不替业务决定「哪些错误该重试、哪些该切换、哪些该终止」</b>——只提供这个策略扩展点。
 * 具体规则（限流 / 余额不足 / 无权限 / 自定义错误码等分别对应哪种处置）由调用方通过
 * {@link KeywordFailoverErrorClassifier} 等实现注入（如产品侧从配置项加载
 * {@code 关键词→动作} 映射）。</p>
 *
 * <p>入参是错误信息文本（流式走 {@code onError(String)}，非流式是异常的 {@code getMessage()}）；
 * 上游端口已把限流 / 余额 / 鉴权等统一以错误文本形式上抛，分类器据此匹配即可。</p>
 *
 * <p>典型用法：</p>
 * <ul>
 *   <li>{@link #failOverAll()}：任何候选失败都切下一候选（中性默认，保留「主模型不行就换辅助」语义）；</li>
 *   <li>{@link KeywordFailoverErrorClassifier}：按配置的关键词 / 错误码映射到
 *       {@link FailoverAction#RETRY} / {@link FailoverAction#FAILOVER} / {@link FailoverAction#ABORT}，
 *       例如把 {@code 429 / rate_limit / 503 / timeout} 配成重试，{@code 401 / 403 / insufficient / balance}
 *       配成切换，{@code 400 / bad_request} 配成终止。</li>
 * </ul>
 */
@FunctionalInterface
public interface FailoverErrorClassifier {

	/**
	 * 该错误对应的处置动作。
	 *
	 * @param errorMessage 错误信息（可能为 {@code null}）
	 * @return 处置动作；返回 {@code null} 时按 {@link FailoverAction#FAILOVER} 处理
	 */
	FailoverAction classify(String errorMessage);

	/** 中性默认：任何错误都切换（与「主模型不行就切辅助」的原始语义一致）。 */
	static FailoverErrorClassifier failOverAll() {
		return message -> FailoverAction.FAILOVER;
	}
}
