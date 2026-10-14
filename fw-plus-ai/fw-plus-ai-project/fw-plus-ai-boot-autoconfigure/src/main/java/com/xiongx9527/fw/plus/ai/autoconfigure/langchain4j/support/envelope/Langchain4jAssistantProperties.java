package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 助手（信封 + 结构化 + 分类 + 会话）配置项，前缀 {@code fw.plus.ai.langchain4j.assistant}。
 *
 * <pre>{@code
 * fw.plus.ai.langchain4j.assistant:
 *   envelope:
 *     enabled: false                     # 帧形态开关（默认关）；开启后既有无 mapper 流式入口的 done 帧承载信封
 *   structured:
 *     default-enabled: false             # 结构化全局默认（默认关）；覆盖顺序：单次调用 > 业务契约 > 全局默认
 *     max-attempts: 3                    # 结构化重试上限（含首次），必须 >= 1
 *   classifier:
 *     enabled: true                      # 分类开关（默认开）；关闭后所有轮次落兜底 chat 且不产生分类调用
 *     history-user-messages: 3           # 分类输入附带的最近用户消息条数，必须 >= 0（0 = 只看本轮）
 *     max-attempts: 1                    # 分类判定尝试次数（含首次），必须 >= 1（默认不重试）
 *   session:
 *     window-size: 20                    # 会话窗口条数（读快照/写回用），必须 >= 1
 * }</pre>
 *
 * <p><b>非法配置的处置</b>：启动阶段（{@link #validate()}，由 {@link InitializingBean} 在绑定完成后自动调用）
 * 发现非法配置立即抛 {@link IllegalStateException} 并指出具体配置项，让应用启动失败；
 * 运行阶段（{@code trySetXxx(...)}，供配置中心推送 / 脚本热更新）遇到非法值保留旧值、记录告警并返回
 * {@code false}，不中断既有服务。</p>
 */
@Slf4j
@ConfigurationProperties(prefix = Langchain4jAssistantProperties.PREFIX)
public class Langchain4jAssistantProperties implements InitializingBean {

	/** 配置前缀 */
	public static final String PREFIX = "fw.plus.ai.langchain4j.assistant";

	private final Envelope envelope = new Envelope();

	private final Structured structured = new Structured();

	private final Classifier classifier = new Classifier();

	private final Session session = new Session();

	@Override
	public void afterPropertiesSet() {
		validate();
	}

	/** 校验全部配置项；非法立即失败并指出配置项（启动期调用） */
	public void validate() {
		envelope.validate();
		structured.validate();
		classifier.validate();
		session.validate();
	}

	public Envelope getEnvelope() {
		return envelope;
	}

	public Structured getStructured() {
		return structured;
	}

	public Classifier getClassifier() {
		return classifier;
	}

	public Session getSession() {
		return session;
	}

	// -------------------------------------------------- 内部工具

	/** 非法配置异常：带上完整配置项与取值 */
	static IllegalStateException illegal(String key, Object value, String reason) {
		return new IllegalStateException("非法配置 " + PREFIX + "." + key + "=" + value + "：" + reason);
	}

	/** 运行期非法值告警：沿用旧值，不中断服务 */
	static void warnIllegal(String key, Object value, String reason) {
		log.warn("忽略非法配置 {}={}：{}（沿用旧值）", PREFIX + "." + key, value, reason);
	}

	// -------------------------------------------------- 帧形态开关

	/** 帧形态：{@code done} 帧承载信封还是模型原文 */
	public static class Envelope {

		/** 帧形态开关（默认 {@code false}：既有输出与调用方行为一字不变） */
		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void validate() {
			// boolean 无非法取值；不可识别的取值由 Spring 绑定阶段拦下（fail-fast 并指出配置项）
		}
	}

	// -------------------------------------------------- 结构化开关

	/** 结构化输出相关：全局默认与重试上限 */
	public static class Structured {

		/** 全局默认是否产出结构化结果（默认 {@code false}） */
		private boolean defaultEnabled = false;

		/** 重试上限（含首次，默认 {@value #DEFAULT_MAX_ATTEMPTS}） */
		private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

		/** 重试上限默认值 */
		public static final int DEFAULT_MAX_ATTEMPTS = 3;

		/**
		 * 三层覆盖顺序解析：单次调用 > 业务契约 > 全局默认。
		 *
		 * @param callOverride     单次调用显式声明；{@code null} 表示未声明
		 * @param contractOverride 业务契约声明；{@code null} 表示未声明
		 * @return 本轮是否产出结构化结果
		 */
		public boolean resolveEnabled(Boolean callOverride, Boolean contractOverride) {
			if (callOverride != null) {
				return callOverride;
			}
			if (contractOverride != null) {
				return contractOverride;
			}
			return defaultEnabled;
		}

		public boolean isDefaultEnabled() {
			return defaultEnabled;
		}

		public void setDefaultEnabled(boolean defaultEnabled) {
			this.defaultEnabled = defaultEnabled;
		}

		public int getMaxAttempts() {
			return maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public void validate() {
			if (maxAttempts < 1) {
				throw illegal("structured.max-attempts", maxAttempts, "结构化重试上限必须 >= 1（含首次）");
			}
		}

		/** 运行期热更新：非法值保留旧值并告警 */
		public boolean trySetMaxAttempts(int value) {
			if (value < 1) {
				warnIllegal("structured.max-attempts", value, "结构化重试上限必须 >= 1（含首次）");
				return false;
			}
			this.maxAttempts = value;
			return true;
		}
	}

	// -------------------------------------------------- 分类

	/** 业务分类相关配置 */
	public static class Classifier {

		/** 分类开关（默认 {@code true}） */
		private boolean enabled = true;

		/** 分类输入附带的最近用户消息条数（默认 {@value #DEFAULT_HISTORY_USER_MESSAGES}；0 = 只看本轮） */
		private int historyUserMessages = DEFAULT_HISTORY_USER_MESSAGES;

		/** 判定尝试次数（含首次，默认 {@value #DEFAULT_MAX_ATTEMPTS}：不重试） */
		private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

		/** 分类历史条数默认值 */
		public static final int DEFAULT_HISTORY_USER_MESSAGES = 3;

		/** 分类尝试次数默认值（不重试） */
		public static final int DEFAULT_MAX_ATTEMPTS = 1;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getHistoryUserMessages() {
			return historyUserMessages;
		}

		public void setHistoryUserMessages(int historyUserMessages) {
			this.historyUserMessages = historyUserMessages;
		}

		public int getMaxAttempts() {
			return maxAttempts;
		}

		public void setMaxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public void validate() {
			if (historyUserMessages < 0) {
				throw illegal("classifier.history-user-messages", historyUserMessages, "分类历史条数必须 >= 0（0 = 只看本轮）");
			}
			if (maxAttempts < 1) {
				throw illegal("classifier.max-attempts", maxAttempts, "分类尝试次数必须 >= 1（含首次）");
			}
		}

		/** 运行期热更新：非法值保留旧值并告警 */
		public boolean trySetHistoryUserMessages(int value) {
			if (value < 0) {
				warnIllegal("classifier.history-user-messages", value, "分类历史条数必须 >= 0（0 = 只看本轮）");
				return false;
			}
			this.historyUserMessages = value;
			return true;
		}

		/** 运行期热更新：非法值保留旧值并告警 */
		public boolean trySetMaxAttempts(int value) {
			if (value < 1) {
				warnIllegal("classifier.max-attempts", value, "分类尝试次数必须 >= 1（含首次）");
				return false;
			}
			this.maxAttempts = value;
			return true;
		}
	}

	// -------------------------------------------------- 会话

	/** 会话历史相关配置 */
	public static class Session {

		/**
		 * 会话窗口条数（默认 {@value #DEFAULT_WINDOW_SIZE}）。
		 *
		 * <p>只作用于信封编排侧的读历史/写回（{@link EnvelopeSessionMemory}）；既有 AiServices 通道的窗口
		 * 由自身配置决定（{@code AssistantBuilder.memory(...)}）。两者默认相同（20）且共用同一个
		 * {@code ChatMemoryStore} 与 {@code memoryId}，但只调其中一个会让同一会话在两条通道上出现
		 * 不同的窗口长度——要么成对配置，要么保持默认。</p>
		 */
		private int windowSize = DEFAULT_WINDOW_SIZE;

		/** 会话窗口条数默认值 */
		public static final int DEFAULT_WINDOW_SIZE = 20;

		public int getWindowSize() {
			return windowSize;
		}

		public void setWindowSize(int windowSize) {
			this.windowSize = windowSize;
		}

		public void validate() {
			if (windowSize < 1) {
				throw illegal("session.window-size", windowSize, "会话窗口条数必须 >= 1");
			}
		}

		/** 运行期热更新：非法值保留旧值并告警 */
		public boolean trySetWindowSize(int value) {
			if (value < 1) {
				warnIllegal("session.window-size", value, "会话窗口条数必须 >= 1");
				return false;
			}
			this.windowSize = value;
			return true;
		}
	}
}
