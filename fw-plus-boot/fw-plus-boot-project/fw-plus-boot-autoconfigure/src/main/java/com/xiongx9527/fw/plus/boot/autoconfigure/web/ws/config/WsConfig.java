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
 * （如 {@code token.secret}）取值，由 {@link #flatten()} 负责展开。</p>
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
	 * 默认应用 ID。
	 */
	private String appId = "default";

	private Token token = new Token();

	private Websocket websocket = new Websocket();

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
	 * 把嵌套配置展开为点号扁平键，例如 {@code token.secret}、{@code session.cache.expireMinutes}。
	 *
	 * <p>内部统一按点号扁平键读取配置，而 Spring Boot 会把 {@code fw.plus.ws.token.secret}
	 * 这类嵌套配置绑定成嵌套对象，因此读取前先展开。键名是读取侧的硬契约，这里显式逐项 put，
	 * 不使用反射或序列化，避免字段名调整时静默丢键。</p>
	 *
	 * @return 点号扁平键的配置映射
	 */
	public Map<String, Object> flatten() {
		Map<String, Object> flat = new HashMap<>(64);
		flat.put("enabled", isEnabled());
		flat.put("appId", appId);

		flat.put("token.secret", token.getSecret());
		flat.put("token.expireHours", token.getExpireHours());

		flat.put("websocket.path", websocket.getPath());
		flat.put("websocket.trustedDomain", websocket.getTrustedDomain());
		flat.put("websocket.subProtocols", websocket.getSubProtocols());
		flat.put("websocket.paths", websocket.getPaths());
		flat.put("websocket.supportsPartialMessages", websocket.isSupportsPartialMessages());
		flat.put("websocket.endpoint", websocket.getEndpoint());
		flat.put("websocket.basePath", websocket.getBasePath());

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

		flat.put("param.token", param.getToken());
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
	 * Token 相关配置。
	 */
	@Data
	public static class Token {

		/**
		 * JWT 密钥，必填；缺失时连接鉴权会失败。
		 */
		private String secret = "";

		/**
		 * Token 有效期（小时）。
		 */
		private long expireHours = 2L;
	}

	/**
	 * 端点相关配置。
	 */
	@Data
	public static class Websocket {

		/**
		 * 端点路径。
		 */
		private String path = "/ws/api";

		/**
		 * 追加端点路径。
		 *
		 * <p>除 {@link #path} 之外，可在同一个处理器上再挂若干路径，供多应用、多入口复用一套 WS 地基；
		 * 与 {@link #path} 重复的项会被去重。webmvc 与 webflux 两条栈均按同一份配置注册。</p>
		 */
		private List<String> paths = new ArrayList<>();

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

		/**
		 * 兼容占位端点，当前不参与注册。
		 */
		private String endpoint = "/ws/execute";

		/**
		 * 兼容占位基础路径，当前不参与注册。
		 */
		private String basePath = "";
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
		 * Token 的 query 参数名。
		 */
		private String token = "token";

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
