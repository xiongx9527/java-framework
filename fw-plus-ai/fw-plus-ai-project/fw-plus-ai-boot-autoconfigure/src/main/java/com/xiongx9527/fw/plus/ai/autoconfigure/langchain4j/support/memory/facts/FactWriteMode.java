package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

/**
 * 事实写入模式：控制抽取/写入的触发时机。
 */
public enum FactWriteMode {

	/** 每轮末即时提交（默认） */
	PER_TURN("per-turn"),

	/** 会话级缓冲，攒满 {@code batch-size} 轮或会话切换时集中提交 */
	BATCHED("batched"),

	/** 会话进行中不写入，会话结束（同用户出现新会话键）时集中提交 */
	SESSION_END("session-end");

	private final String configName;

	FactWriteMode(String configName) {
		this.configName = configName;
	}

	public String getConfigName() {
		return configName;
	}

	/** 配置名 → 模式（忽略大小写与空白）；{@code null} / 未知回退 {@link #PER_TURN}（属性层已做合法性校验）。 */
	public static FactWriteMode fromConfig(String name) {
		if (name != null) {
			String trimmed = name.trim();
			for (FactWriteMode mode : values()) {
				if (mode.configName.equalsIgnoreCase(trimmed)) {
					return mode;
				}
			}
		}
		return PER_TURN;
	}
}
