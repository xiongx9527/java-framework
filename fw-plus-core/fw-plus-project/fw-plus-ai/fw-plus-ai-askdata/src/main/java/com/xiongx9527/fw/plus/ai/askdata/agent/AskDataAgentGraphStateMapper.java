package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.List;

/**
 * 将外部运行时的脱敏节点结果映射为 Core 图状态。
 *
 * <p>该参考映射器只判断结果集合是否存在，不代表节点级成功判定。外部 Adapter 必须在调用前处理框架异常、
 * 超时和取消，并将其映射为对应 Core 终态；禁止把部分结果存在等同于完整成功。</p>
 */
public final class AskDataAgentGraphStateMapper {

	private final AskDataAgentGraphState state;

	private AskDataAgentGraphStateMapper(AskDataAgentGraphState state) {
		this.state = state;
	}

	public static AskDataAgentGraphStateMapper fromResults(List<String> results) {
		return new AskDataAgentGraphStateMapper(results == null
				? AskDataAgentGraphState.FAILED : AskDataAgentGraphState.COMPLETED);
	}

	public AskDataAgentGraphState getState() {
		return state;
	}
}
