package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 个人信息提供者（样例用内存 mock，生产换成查库或调用户中心）。
 *
 * <p>带按 memoryId 的短 TTL 缓存：profile 每轮都会被读取，不缓存等于每轮一次 IO，
 * 直接加到首字延迟上。TTL 到期自动回源，因此修改数据源后在 TTL 内生效。</p>
 *
 * <p>注意只放「影响回答的」字段——称呼、时区、语言、角色。手机号、身份证、住址
 * 不要进来：模型用不到，却会把泄漏面扩大到 prompt 与日志全链路。</p>
 */
@Component
public class UserProfileProvider {

	/** 缓存有效期：profile 变更最长在此时间后对模型生效。 */
	private static final long CACHE_MILLIS = TimeUnit.SECONDS.toMillis(60);

	/** 数据源（mock）。 */
	private final Map<Object, UserProfile> source = new ConcurrentHashMap<>();

	/** 缓存：value 是 [profile, 写入时间戳]。 */
	private final Map<Object, Object[]> cache = new ConcurrentHashMap<>();

	/**
	 * 取 profile；未注册的 memoryId 返回 {@code null}，调用方需处理「没有资料」的情况。
	 */
	public UserProfile find(Object memoryId) {
		Object[] cached = cache.get(memoryId);
		long now = System.currentTimeMillis();
		if (cached != null && now - (Long) cached[1] < CACHE_MILLIS) {
			return (UserProfile) cached[0];
		}

		UserProfile profile = source.get(memoryId);
		if (profile == null) {
			cache.remove(memoryId);
			return null;
		}
		cache.put(memoryId, new Object[]{profile, now});
		return profile;
	}

	/** 登记/修改 profile，并立即使该 memoryId 的缓存失效。 */
	public void save(Object memoryId, UserProfile profile) {
		source.put(memoryId, profile);
		cache.remove(memoryId);
	}

	/** 个人资料：只放会影响模型回答的字段。 */
	public static class UserProfile {

		private final String displayName;
		private final String timezone;
		private final String preference;

		public UserProfile(String displayName, String timezone, String preference) {
			this.displayName = displayName;
			this.timezone = timezone;
			this.preference = preference;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getTimezone() {
			return timezone;
		}

		public String getPreference() {
			return preference;
		}

		/**
		 * 渲染进系统提示的片段；无资料时返回 {@code null}，由调用方决定是否加这一段。
		 */
		public String render() {
			return "当前用户：" + displayName + "，所在时区：" + timezone + "，偏好：" + preference;
		}
	}
}
