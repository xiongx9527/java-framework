package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 通用配置，绑定前缀 {@code fw.plus.ws}。
 *
 * <p>该配置类不引用任何 magic-api 类型，位于 {@code com.xiongx9527.fw.plus.boot.autoconfigure.web.ws}
 * 包内，任何应用都可以直接配置 {@code fw.plus.ws.*}。</p>
 *
 * <p>属性为类型安全 POJO，字段默认值即框架缺省值。内部读取侧统一按点号扁平键
 * （如 {@code session.cache.expireMinutes}）取值，由 {@link #flatten()} 负责展开。</p>
 *
 * @author zzy
 */
@Data
@RefreshScope
@ConfigurationProperties("fw.plus.ws")
public class WsConfig {

	/**
	 * 是否启用 WebSocket 地基。
	 */
	private boolean enabled = true;

	/**
	 * 默认应用 ID：默认实例与未填写 appId 的命名实例都回落到它。
	 */
	private String appId = "default";

	/**
	 * 默认实例的主端点路径；相对路径（如 {@code /ws/api}）与含基础路径的完整路径均可。
	 *
	 * <p>显式置空串表示「只按基础路径注册」，与命名实例 {@link WsInstance#getPath()} 的语义一致。</p>
	 */
	private String path = "/ws/api";

	/**
	 * 默认实例的追加端点路径。
	 *
	 * <p>除 {@link #path} 之外，可在同一个处理器上再挂若干路径，供多应用、多入口复用一套 WS 地基；
	 * 与 {@link #path} 重复的项会被去重。webmvc 与 webflux 两条栈均按同一份配置注册。</p>
	 */
	private List<String> paths = new ArrayList<>();

	private Websocket websocket = new Websocket();

	/**
	 * 命名 WebSocket 实例列表，一个实例 = 一个 key + 一组端点 + 一个 appId。
	 *
	 * <p>列表在默认实例之外做纯增量：默认实例始终存在，端点取 {@link #getPath()} 与
	 * {@link #getPaths()}，appId 取 {@link #getAppId()}，因此不配该列表时行为与改造前
	 * 完全一致。</p>
	 *
	 * <p>握手时按请求路径命中实例，命中后 appId 取 {@link WsInstance#getAppId()}（留空回落
	 * {@link #getAppId()}），未命中任何实例也回落默认实例。实例路径可写相对路径（如
	 * {@code /ws/api2}），也可写含基础路径的完整路径，两者都能命中。</p>
	 *
	 * <p>该列表不进入 {@link #flatten()} 的扁平键：读取侧由 {@code WsInstanceResolver}
	 * 直接按对象解析，扁平键保持纯标量契约。</p>
	 */
	private List<WsInstance> servers = new ArrayList<>();

	private Session session = new Session();

	private Scheduler scheduler = new Scheduler();

	private Redis redis = new Redis();

	private Param param = new Param();

	private Init init = new Init();

	private Close close = new Close();

	private Binary binary = new Binary();

	private Broadcast broadcast = new Broadcast();

	private Webflux webflux = new Webflux();

	/**
	 * 把嵌套配置展开为点号扁平键，例如 {@code appId}、{@code session.cache.expireMinutes}。
	 *
	 * <p>内部统一按点号扁平键读取配置，而 Spring Boot 会把
	 * {@code fw.plus.ws.session.cache.maxClients} 这类嵌套配置绑定成嵌套对象，因此读取前先展开。
	 * 键名是读取侧的硬契约，这里显式逐项 put，不使用反射或序列化，避免字段名调整时静默丢键。</p>
	 *
	 * @return 点号扁平键的配置映射
	 */
	public Map<String, Object> flatten() {
		Map<String, Object> flat = new HashMap<>(64);
		flat.put("enabled", isEnabled());
		flat.put("appId", appId);

		flat.put("path", path);
		flat.put("paths", paths);
		flat.put("websocket.trustedDomain", websocket.getTrustedDomain());
		flat.put("websocket.subProtocols", websocket.getSubProtocols());
		flat.put("websocket.supportsPartialMessages", websocket.isSupportsPartialMessages());

		flat.put("session.cache.maxClients", session.getCache().getMaxClients());
		flat.put("session.cache.expireMinutes", session.getCache().getExpireMinutes());
		flat.put("session.cache.concurrencyLevel", session.getCache().getConcurrencyLevel());
		flat.put("session.delayMillis", session.getDelayMillis());
		flat.put("session.maxTextMessageSize", session.getMaxTextMessageSize());
		flat.put("session.maxBinaryMessageSize", session.getMaxBinaryMessageSize());
		flat.put("session.maxTotalSessions", session.getMaxTotalSessions());
		flat.put("session.maxSessionsPerClient", session.getMaxSessionsPerClient());
		flat.put("session.idleScanEnabled", session.isIdleScanEnabled());
		flat.put("session.idleScanIntervalSeconds", session.getIdleScanIntervalSeconds());
		flat.put("session.closeOnCacheEviction", session.isCloseOnCacheEviction());

		flat.put("scheduler.delayedClosePoolSize", scheduler.getDelayedClosePoolSize());

		flat.put("redis.channel.broadcast", redis.getChannel().getBroadcast());

		flat.put("param.clientId", param.getClientId());
		flat.put("param.maxClientIdLength", param.getMaxClientIdLength());

		flat.put("init.type", init.getType());
		flat.put("close.type", close.getType());

		flat.put("binary.enabled", binary.isEnabled());
		flat.put("binary.type", binary.getType());

		flat.put("broadcast.async", broadcast.isAsync());

		flat.put("webflux.scriptThreadPool.coreSize", webflux.getScriptThreadPool().getCoreSize());
		flat.put("webflux.scriptThreadPool.maxSize", webflux.getScriptThreadPool().getMaxSize());
		flat.put("webflux.scriptThreadPool.queueCapacity", webflux.getScriptThreadPool().getQueueCapacity());
		return flat;
	}

	/**
	 * 单个命名 WebSocket 实例：一个 key + 一组端点 + 一个 appId。
	 */
	@Data
	public static class WsInstance {

		/**
		 * 实例标识：必填非空，trim 后在 {@code servers} 内全局唯一，否则启动失败。
		 *
		 * <p>仅用于配置识别、日志与端点冲突报错，不参与握手匹配。</p>
		 */
		private String key = "";

		/**
		 * 实例的主端点路径；相对路径（如 {@code /ws/api2}）或含基础路径的完整路径均可。
		 */
		private String path = "";

		/**
		 * 实例的追加端点路径，与 {@link #getPath()} 挂在同一套处理器上；重复项按配置顺序去重。
		 */
		private List<String> paths = new ArrayList<>();

		/**
		 * 该实例的应用 ID；留空表示回落全局 {@code fw.plus.ws.appId}。
		 */
		private String appId = "";
	}

	/**
	 * 握手参数配置。
	 */
	@Data
	public static class Websocket {

		/**
		 * 受信任来源，逗号分隔；空串表示不允许跨域。
		 */
		private String trustedDomain = "";

		/**
		 * 子协议，逗号分隔。
		 */
		private String subProtocols = "";

		/**
		 * 是否支持部分消息。
		 */
		private boolean supportsPartialMessages = false;
	}

	/**
	 * 会话相关配置。
	 */
	@Data
	public static class Session {

		/**
		 * 延迟关闭毫秒数。
		 */
		private int delayMillis = 1000;

		/**
		 * 单条文本帧上限（字节）。
		 */
		private int maxTextMessageSize = 65536;

		/**
		 * 单条二进制帧上限（字节）。
		 */
		private int maxBinaryMessageSize = 65536;

		/**
		 * 全局会话数上限，超出后拒绝新握手。
		 */
		private int maxTotalSessions = 10000;

		/**
		 * 单个 clientId 下会话数上限，超出后拒绝新握手。
		 */
		private int maxSessionsPerClient = 100;

		/**
		 * 是否开启空闲会话定时扫描，用于回收不发 FIN 就掉线的连接。
		 */
		private boolean idleScanEnabled = false;

		/**
		 * 空闲扫描周期（秒）。
		 */
		private int idleScanIntervalSeconds = 60;

		/**
		 * 缓存驱逐时是否主动关闭仍打开的会话；默认 false，避免误杀长连接。
		 */
		private boolean closeOnCacheEviction = false;

		private Cache cache = new Cache();

		/**
		 * 会话缓存配置。
		 */
		@Data
		public static class Cache {

			/**
			 * 缓存中 clientId 的最大数量。
			 */
			private int maxClients = 10000;

			/**
			 * 会话缓存过期时间与空闲判定阈值（分钟）。
			 */
			private int expireMinutes = 30;

			/**
			 * 缓存并发级别。
			 */
			private int concurrencyLevel = 16;
		}
	}

	/**
	 * 调度器配置。
	 */
	@Data
	public static class Scheduler {

		/**
		 * 延迟关闭调度线程池大小。
		 */
		private int delayedClosePoolSize = 2;
	}

	/**
	 * Redis 广播配置。
	 */
	@Data
	public static class Redis {

		private Channel channel = new Channel();

		/**
		 * Redis 频道配置。
		 */
		@Data
		public static class Channel {

			/**
			 * 跨实例广播频道名。
			 */
			private String broadcast = "fw-plus:websocket:broadcast";
		}
	}

	/**
	 * 握手参数名配置。
	 */
	@Data
	public static class Param {

		/**
		 * 客户端 ID 的 query 参数名。
		 */
		private String clientId = "clientId";

		/**
		 * 客户端 ID 最大长度。
		 */
		private int maxClientIdLength = 50;
	}

	/**
	 * 建连阶段配置。
	 */
	@Data
	public static class Init {

		/**
		 * 建连订阅的业务类型。
		 */
		private String type = "init";
	}

	/**
	 * 断连阶段配置。
	 */
	@Data
	public static class Close {

		/**
		 * 断连订阅的业务类型。
		 */
		private String type = "close";
	}

	/**
	 * 二进制消息配置。
	 */
	@Data
	public static class Binary {

		/**
		 * 是否接受二进制帧；为 false 时以 NOT_ACCEPTABLE 关闭。
		 */
		private boolean enabled = true;

		/**
		 * 二进制帧使用的业务类型。
		 */
		private String type = "binary";
	}

	/**
	 * 广播配置。
	 */
	@Data
	public static class Broadcast {

		/**
		 * 本地广播是否异步；默认 false，保持与改造前一致的同步投递。
		 */
		private boolean async = false;
	}

	/**
	 * WebFlux 栈配置。
	 */
	@Data
	public static class Webflux {

		private ScriptThreadPool scriptThreadPool = new ScriptThreadPool();

		/**
		 * 订阅脚本执行线程池配置：脚本同步阻塞，必须移出 Reactor event loop。
		 */
		@Data
		public static class ScriptThreadPool {

			private int coreSize = 8;

			private int maxSize = 32;

			private int queueCapacity = 1000;
		}
	}
}
