package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session;

/**
 * 会话标识解析器工厂：按配置名得到实现（{@code daily} / {@code explicit}），
 * 名字缺失或未知时回退 {@code daily}，保证会话键恒为五段。
 */
public final class SessionIdResolvers {

	public static final String DAILY = "daily";
	public static final String EXPLICIT = "explicit";

	private SessionIdResolvers() {
	}

	/**
	 * 解析会话解析器。
	 *
	 * @param name       配置名（{@code daily} / {@code explicit}）；空或未知回退 {@code daily}
	 * @param explicitId {@code explicit} 模式下使用的显式会话标识（可为 null）
	 */
	public static SessionIdResolver resolve(String name, String explicitId) {
		if (name != null && EXPLICIT.equalsIgnoreCase(name.trim())) {
			return new ExplicitSessionIdResolver(explicitId);
		}
		return new DailySessionIdResolver();
	}

}	