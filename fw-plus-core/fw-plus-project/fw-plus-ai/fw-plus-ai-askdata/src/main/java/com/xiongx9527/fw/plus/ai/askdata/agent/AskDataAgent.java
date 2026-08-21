package com.xiongx9527.fw.plus.ai.askdata.agent;

/**
 * AskData 中立 Agent SPI。
 *
 * <p>旧实现可以继续只提供能力 key；只有返回受支持步骤 key 的 Agent 才会被步骤注册表选为默认 Flow
 * 的替换实现。</p>
 */
public interface AskDataAgent {

	String getKey();

	default String getStepKey() {
		return null;
	}

	default int getPriority() {
		return 0;
	}

	default AskDataAgentStepResult execute(AskDataAgentStepContext context) {
		throw new UnsupportedOperationException("Agent 未实现稳定步骤执行: " + getKey());
	}
}
