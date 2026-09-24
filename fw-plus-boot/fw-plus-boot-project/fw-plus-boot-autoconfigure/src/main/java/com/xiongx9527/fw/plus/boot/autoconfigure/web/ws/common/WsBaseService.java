package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 配置访问与 JWT 工具。
 *
 * <p>配置统一按 {@code fw.plus.ws} 前缀绑定，内部按点号扁平键读取。该接口位于
 * {@code com.xiongx9527.fw.plus.boot.autoconfigure.web.ws} 包内，不引用任何 magic-api 类型。</p>
 *
 * @author zzy
 */
public interface WsBaseService {

    /**
     * 获取 Token 参数名。
     *
     * @return Token 参数名，缺省 {@code token}
     */
    default String getTokenParamName() {
        return getParamNameFromConfig("param.token", "token");
    }

    /**
     * 获取客户端 ID 参数名。
     *
     * @return 客户端 ID 参数名，缺省 {@code clientId}
     */
    default String getClientIdParamName() {
        return getParamNameFromConfig("param.clientId", "clientId");
    }

    /**
     * 获取客户端 ID 的最大长度限制。
     *
     * @return 最大长度，缺省 50
     */
    default int getMaxClientIdLength() {
        return getConfigValueAsInt("param.maxClientIdLength", 50);
    }

    /**
     * 从配置中获取参数名。
     *
     * @param configKey    配置键名
     * @param defaultValue 默认参数名
     * @return 参数名
     */
    default String getParamNameFromConfig(String configKey, String defaultValue) {
        return getConfigValueAsString(configKey, defaultValue);
    }

    /**
     * 获取配置信息。
     *
     * <p>配置对象按 {@code fw.plus.ws} 前缀绑定，嵌套写法（{@code fw.plus.ws.token.secret}）在绑定后
     * 表现为嵌套 Map；这里统一展开为点号扁平键，供按 {@code token.secret} 取值。</p>
     *
     * @return 配置信息映射（点号扁平键）
     */
    default Map<String, Object> getConfig() {
        WsConfig config = SpringUtils.getBean(WsConfig.class);
        if (config != null) {
            return config.flatten();
        }
        return new HashMap<>();
    }

    /**
     * 从配置中获取长整型值。
     */
    default long getConfigValueAsLong(String configKey, long defaultValue) {
        return WsConfigReader.getAsLong(getConfig(), configKey, defaultValue);
    }

    /**
     * 从配置中获取整型值。
     */
    default int getConfigValueAsInt(String configKey, int defaultValue) {
        return WsConfigReader.getAsInt(getConfig(), configKey, defaultValue);
    }

    /**
     * 从配置中获取字符串值。
     */
    default String getConfigValueAsString(String configKey, String defaultValue) {
        return WsConfigReader.getAsString(getConfig(), configKey, defaultValue);
    }

    /**
     * 从配置中获取布尔值。
     */
    default boolean getConfigValueAsBoolean(String configKey, boolean defaultValue) {
        return WsConfigReader.getAsBoolean(getConfig(), configKey, defaultValue);
    }

    /**
     * 获取广播频道名称。
     *
     * @return 广播频道名称，缺省 {@code websocket:broadcast}
     */
    default String getBroadcastChannelName() {
        return getConfigValueAsString("redis.channel.broadcast", "websocket:broadcast");
    }

    /**
     * 获取 WebSocket 路径。
     *
     * @return WebSocket 路径，缺省 {@code /ws/api}
     */
    default String getWebsocketPath() {
        return getConfigValueAsString("websocket.path", "/ws/api");
    }

    /**
     * 获取 WebSocket 受信任的域名。
     *
     * @return 受信任的域名列表，缺省空串（不允许跨域）
     */
    default String getTrustedDomain() {
        return getConfigValueAsString("websocket.trustedDomain", "");
    }

    /**
     * 对敏感值做日志脱敏，避免 token、secret 等凭证被完整写入日志。
     *
     * @param value 敏感值
     * @return 脱敏后的值
     */
    default String maskSensitiveValue(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        if (value.length() <= 8) {
            return "***";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    /**
     * 获取 WebSocket 子协议数组。
     *
     * @return 子协议字符串数组，未配置时返回空数组
     */
    default String[] getSubProtocols() {
        String subProtocols = getConfigValueAsString("websocket.subProtocols", "");
        if (StringUtils.isNotBlank(subProtocols)) {
            List<String> protocols = new ArrayList<>();
            for (String protocol : subProtocols.split(",")) {
                String trimmedProtocol = protocol.trim();
                if (StringUtils.isNotBlank(trimmedProtocol)) {
                    protocols.add(trimmedProtocol);
                }
            }
            return protocols.toArray(new String[protocols.size()]);
        }
        return new String[0];
    }

    /**
     * 获取 Token 过期时间（小时）。
     *
     * @return Token 过期时间（小时），缺省 2
     */
    default long getTokenExpireHours() {
        return getConfigValueAsLong("token.expireHours", 2L);
    }

    /**
     * 获取 Token 密钥。
     *
     * @return Token 密钥
     */
    default String getTokenSecret() {
        String tokenSecret = getConfigValueAsString("token.secret", "");
        if (StringUtils.isBlank(tokenSecret)) {
            throw new IllegalStateException("fw.plus.ws.token.secret must be configured");
        }
        return tokenSecret;
    }

    /**
     * 根据配置获取 Token 密钥和签名算法。
     *
     * @return 包含密钥和签名算法的对象
     */
    default SecretKeyAndSignatureAlgorithm getSecretKeyAndSignatureAlgorithm() {
        // 构建JWT token，明确指定UTF-8编码以确保跨平台一致性
        // 确保使用适合的签名算法和足够长度的密钥
        byte[] keyBytes = getTokenSecret().getBytes(StandardCharsets.UTF_8);

        // 定义算法所需的最小密钥长度常量
        final int HS512_MIN_KEY_LENGTH = 64;  // 512位
        final int HS384_MIN_KEY_LENGTH = 48;  // 384位
        final int HS256_MIN_KEY_LENGTH = 32;  // 256位

        // 根据密钥长度选择合适的签名算法
        // HS512需要至少512位(64字节)密钥
        // HS384需要至少384位(48字节)密钥
        // HS256需要至少256位(32字节)密钥
        SecretKey secretKey;
        SignatureAlgorithm algorithm;

        SecretKeyAndSignatureAlgorithm secretKeyAndSignatureAlgorithm = new SecretKeyAndSignatureAlgorithm();
        if (keyBytes.length >= HS512_MIN_KEY_LENGTH) {
            // 密钥长度足够支持HS512
            secretKey = Keys.hmacShaKeyFor(keyBytes);
            algorithm = io.jsonwebtoken.SignatureAlgorithm.HS512;
        } else if (keyBytes.length >= HS384_MIN_KEY_LENGTH) {
            // 密钥长度足够支持HS384
            secretKey = Keys.hmacShaKeyFor(keyBytes);
            algorithm = io.jsonwebtoken.SignatureAlgorithm.HS384;
        } else if (keyBytes.length >= HS256_MIN_KEY_LENGTH) {
            // 密钥长度足够支持HS256
            secretKey = Keys.hmacShaKeyFor(keyBytes);
            algorithm = io.jsonwebtoken.SignatureAlgorithm.HS256;
        } else {
            // 密钥长度不足，使用标准方法生成安全密钥
            // 为HS512生成64字节密钥
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-512");
                byte[] hashedBytes = digest.digest(keyBytes);
                // 确保我们有64字节用于HS512，使用完整的哈希结果（通常为64字节）
                secretKey = Keys.hmacShaKeyFor(hashedBytes);
                algorithm = SignatureAlgorithm.HS512;
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate secure key", e);
            }
        }
        secretKeyAndSignatureAlgorithm.secretKey = secretKey;
        secretKeyAndSignatureAlgorithm.algorithm = algorithm;
        return secretKeyAndSignatureAlgorithm;
    }

    /**
     * 验证 JWT token 的有效性并提取 appId。
     *
     * @param token JWT token 字符串
     * @return token 有效时返回对应的 appId
     */
    default String validateAndExtractAppIdFromToken(String token) {
        SecretKeyAndSignatureAlgorithm keyAndAlg = getSecretKeyAndSignatureAlgorithm();
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(keyAndAlg.getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate token", e);
        }
    }

    /**
     * 秘钥和签名算法封装类。
     */
    @Data
    class SecretKeyAndSignatureAlgorithm {
        /**
         * JWT签名使用的秘钥
         */
        private SecretKey secretKey;

        /**
         * JWT签名算法
         */
        private SignatureAlgorithm algorithm;
    }

    /**
     * 获取默认的应用 ID。
     *
     * @return 默认应用 ID，缺省 {@code default}
     */
    default String getDefaultAppId() {
        return getConfigValueAsString("appId", "default");
    }

    /**
     * 获取默认的应用 ID。
     *
     * @return 默认应用 ID，缺省 {@code default}
     * @deprecated 名称未表达语义，请改用 {@link #getDefaultAppId()}；保留以免破坏既有宿主。
     */
    @Deprecated
    default String getDefaultKey() {
        return getDefaultAppId();
    }

    /**
     * 获取初始化业务类型。
     *
     * @return 初始化业务类型，缺省 {@code init}
     */
    default String getInitBusinessType() {
        return getConfigValueAsString("init.type", "init");
    }

    /**
     * 获取关闭连接的业务类型。
     *
     * @return 关闭连接的业务类型，缺省 {@code close}
     */
    default String getCloseBusinessType() {
        return getConfigValueAsString("close.type", "close");
    }

    /**
     * 获取初始化业务类型。
     *
     * @return 初始化业务类型，缺省 {@code init}
     * @deprecated 拼写错误，请改用 {@link #getInitBusinessType()}；保留以免破坏既有宿主。
     */
    @Deprecated
    default String getInitBussinessType() {
        return getInitBusinessType();
    }

    /**
     * 获取关闭连接的业务类型。
     *
     * @return 关闭连接的业务类型，缺省 {@code close}
     * @deprecated 拼写错误，请改用 {@link #getCloseBusinessType()}；保留以免破坏既有宿主。
     */
    @Deprecated
    default String getCloseBussinessType() {
        return getCloseBusinessType();
    }

    /**
     * 获取 WebSocket 是否支持部分消息的配置。
     *
     * @return true 表示支持部分消息，缺省 false
     */
    default boolean getSupportsPartialMessages() {
        return getConfigValueAsBoolean("websocket.supportsPartialMessages", false);
    }

    /**
     * 获取最大客户端会话数。
     *
     * @return 最大客户端会话数，缺省 10000
     */
    default int getMaxClientSessions() {
        return getConfigValueAsInt("session.cache.maxClients", 10000);
    }

    /**
     * 获取会话缓存过期时间（分钟）。
     *
     * @return 会话缓存过期时间（分钟），缺省 30
     */
    default int getSessionCacheExpireMinutes() {
        return getConfigValueAsInt("session.cache.expireMinutes", 30);
    }

    /**
     * 获取缓存并发级别。
     *
     * @return 缓存并发级别，缺省 16
     */
    default int getCacheConcurrencyLevel() {
        return getConfigValueAsInt("session.cache.concurrencyLevel", 16);
    }

    /**
     * 获取延迟关闭调度线程池大小。
     *
     * @return 线程池大小，缺省 2
     */
    default int getDelayedClosePoolSize() {
        return getConfigValueAsInt("scheduler.delayedClosePoolSize", 2);
    }

    /**
     * 获取会话延迟时间（毫秒）。
     *
     * @return 延迟时间（毫秒），缺省 1000
     */
    default int getDelayMillis() {
        return getConfigValueAsInt("session.delayMillis", 1000);
    }

    /**
     * 是否接受二进制帧。
     *
     * @return true 表示接受，缺省 true
     */
    default boolean isBinaryEnabled() {
        return getConfigValueAsBoolean("binary.enabled", true);
    }

    /**
     * 获取二进制帧使用的业务类型。
     *
     * @return 业务类型，缺省 {@code binary}
     */
    default String getBinaryBusinessType() {
        return getConfigValueAsString("binary.type", "binary");
    }

    /**
     * 获取单条文本帧上限（字节）。
     *
     * @return 上限，缺省 65536
     */
    default int getMaxTextMessageSize() {
        return getConfigValueAsInt("session.maxTextMessageSize", 65536);
    }

    /**
     * 获取单条二进制帧上限（字节）。
     *
     * @return 上限，缺省 65536
     */
    default int getMaxBinaryMessageSize() {
        return getConfigValueAsInt("session.maxBinaryMessageSize", 65536);
    }

    /**
     * 获取全局会话数上限。
     *
     * @return 上限，缺省 10000
     */
    default int getMaxTotalSessions() {
        return getConfigValueAsInt("session.maxTotalSessions", 10000);
    }

    /**
     * 获取单个 clientId 下会话数上限。
     *
     * @return 上限，缺省 100
     */
    default int getMaxSessionsPerClient() {
        return getConfigValueAsInt("session.maxSessionsPerClient", 100);
    }

    /**
     * 是否开启空闲会话定时扫描。
     *
     * @return true 表示开启，缺省 false
     */
    default boolean isIdleScanEnabled() {
        return getConfigValueAsBoolean("session.idleScanEnabled", false);
    }

    /**
     * 获取空闲扫描周期（秒）。
     *
     * @return 周期，缺省 60
     */
    default int getIdleScanIntervalSeconds() {
        return getConfigValueAsInt("session.idleScanIntervalSeconds", 60);
    }

    /**
     * 缓存驱逐时是否主动关闭仍打开的会话。
     *
     * @return true 表示关闭，缺省 false
     */
    default boolean isCloseOnCacheEviction() {
        return getConfigValueAsBoolean("session.closeOnCacheEviction", false);
    }

    /**
     * 本地广播是否异步投递。
     *
     * @return true 表示异步，缺省 false
     */
    default boolean isBroadcastAsync() {
        return getConfigValueAsBoolean("broadcast.async", false);
    }

    /**
     * 获取 WebFlux 栈订阅脚本线程池核心大小。
     *
     * @return 核心大小，缺省 8
     */
    default int getScriptThreadPoolCoreSize() {
        return getConfigValueAsInt("webflux.scriptThreadPool.coreSize", 8);
    }

    /**
     * 获取 WebFlux 栈订阅脚本线程池最大大小。
     *
     * @return 最大大小，缺省 32
     */
    default int getScriptThreadPoolMaxSize() {
        return getConfigValueAsInt("webflux.scriptThreadPool.maxSize", 32);
    }

    /**
     * 获取 WebFlux 栈订阅脚本线程池队列容量。
     *
     * @return 队列容量，缺省 1000
     */
    default int getScriptThreadPoolQueueCapacity() {
        return getConfigValueAsInt("webflux.scriptThreadPool.queueCapacity", 1000);
    }
}
