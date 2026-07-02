package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.starter.MagicAPISseConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

public interface BaseService extends MagicBaseService {

    /**
     * 根据错误码键名获取对应的错误码值
     *
     * @param errorCodeKey 错误码键名
     * @param defaultCode 默认错误码值，当配置中未找到对应键名时使用
     * @return 从配置中获取的错误码值，如果配置中不存在则返回默认值
     */
    default int getErrorCode(String errorCodeKey, int defaultCode) {
        return getConfigValueAsInt("error.code." + errorCodeKey, defaultCode);
    }

    /**
     * 获取Token参数名
     *
     * @return Token参数名，如果配置中未指定则返回默认值"token"
     */
    default String getTokenParamName() {
        return getParamNameFromConfig("param.token", "token");
    }

    /**
     * 获取客户端ID参数名
     *
     * @return 客户端ID参数名，如果配置中未指定则返回默认值"clientId"
     */
    default String getClientIdParamName() {
        return getParamNameFromConfig("param.clientId", "clientId");
    }

    /**
     * 获取客户端ID的最大长度限制
     *
     * @return 客户端ID的最大长度，如果配置中未指定则返回默认值50
     */
    default int getMaxClientIdLength() {
        return getConfigValueAsInt("param.maxClientIdLength",50);
    }

    /**
     * 从配置中获取参数名
     *
     * @param configKey 配置键名
     * @param defaultValue 默认参数名，当配置中未找到对应键名时使用
     * @return 从配置中获取的参数名，如果配置中不存在则返回默认值
     */
    default String getParamNameFromConfig(String configKey, String defaultValue) {
        return getConfigValueAsString(configKey, defaultValue);
    }


    /**
     * 获取配置信息
     * 
     * @return 配置信息映射
     */
    default Map<String, Object> getConfig() {
        MagicAPISseConfig config = SpringUtils.getBean(MagicAPISseConfig.class);
        if (config != null) {
            return config;
        }
        return new HashMap<>();
    }

    /**
     * 从配置中获取长整型值
     *
     * @param configKey 配置键名
     * @param defaultValue 默认长整型值，当配置中未找到对应键名或转换失败时使用
     * @return 从配置中获取并转换的长整型值，如果获取失败则返回默认值
     */
    default long getConfigValueAsLong(String configKey, long defaultValue) {
        return getConfigValueAsLong(getConfig(),configKey, defaultValue);
    }

    /**
     * 从配置中获取整型值
     *
     * @param configKey 配置键名
     * @param defaultValue 默认整型值，当配置中未找到对应键名或转换失败时使用
     * @return 从配置中获取并转换的整型值，如果获取失败则返回默认值
     */
    default int getConfigValueAsInt(String configKey, int defaultValue) {
        return getConfigValueAsInt(getConfig(),configKey, defaultValue);
    }

    /**
     * 从配置中获取字符串值
     *
     * @param configKey 配置键名
     * @param defaultValue 默认字符串值，当配置中未找到对应键名或值为空时使用
     * @return 从配置中获取的字符串值，如果获取失败或值为空则返回默认值
     */
    default String getConfigValueAsString(String configKey, String defaultValue) {
        return getConfigValueAsString(getConfig(),configKey, defaultValue);
    }

    /**
     * 从配置中获取布尔值
     *
     * @param configKey 配置键名
     * @param defaultValue 默认布尔值，当配置中未找到对应键名或转换失败时使用
     * @return 从配置中获取并转换的布尔值，如果获取失败则返回默认值
     */
    default boolean getConfigValueAsBoolean(String configKey, boolean defaultValue) {
        return getConfigValueAsBoolean(getConfig(),configKey, defaultValue);
    }


    /**
     * 获取广播频道名称
     *
     * @return 广播频道名称，如果配置中未指定则返回默认值"websocket:broadcast"
     */
    default String getBroadcastChannelName() {
        return getConfigValueAsString("redis.channel.broadcast","websocket:broadcast");
    }


    /**
     * 获取WebSocket路径
     *
     * @return WebSocket路径，如果配置中未指定则返回默认值"websocket:group"
     */
    default String getWebsocketPath() {
        return getConfigValueAsString("sse.path","/ws/api");
    }

    /**
     * 获取WebSocket受信任的域名
     *
     * @return 受信任的域名列表，如果配置中未指定则返回默认值"*"（允许所有域名）
     */
    default String getTrustedDomain() {
        return getConfigValueAsString("sse.trustedDomain","*");
    }

    /**
     * 获取Token过期时间（小时）
     *
     * @return Token过期时间（小时），如果配置中未指定则返回默认值2小时
     */
    default long getTokenExpireHours() {
        return getConfigValueAsLong("token.expireHours",2L);
    }

    /**
     * 获取Token密钥
     *
     * @return Token密钥，如果配置中未指定则返回默认值"63XYwWBJF5P3GBCZfFLFWXGbSw4KpKSg"
     */
    default String getTokenSecret() {
        return getConfigValueAsString("token.secret","63XYwWBJF5P3GBCZfFLFWXGbSw4KpKSg");
    }


    /**
     * 根据配置获取Token密钥和签名算法
     * 
     * @return 包含密钥和签名算法的SecretKeyAndSignatureAlgorithm对象
     */
    default SecretKeyAndSignatureAlgorithm getSecretKeyAndSignatureAlgorithm() {
        // 构建JWT token，明确指定UTF-8编码以确保跨平台一致性
        // 确保使用适合的签名算法和足够长度的密钥
        byte[] keyBytes = getTokenSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 定义算法所需的最小密钥长度常量
        final int HS512_MIN_KEY_LENGTH = 64;  // 512位
        final int HS384_MIN_KEY_LENGTH = 48;  // 384位
        final int HS256_MIN_KEY_LENGTH = 32;  // 256位

        // 根据密钥长度选择合适的签名算法
        // HS512需要至少512位(64字节)密钥
        // HS384需要至少384位(48字节)密钥
        // HS256需要至少256位(32字节)密钥
        javax.crypto.SecretKey secretKey;
        io.jsonwebtoken.SignatureAlgorithm algorithm;

        SecretKeyAndSignatureAlgorithm secretKeyAndSignatureAlgorithm = new SecretKeyAndSignatureAlgorithm();
        if (keyBytes.length >= HS512_MIN_KEY_LENGTH) {
            // 密钥长度足够支持HS512
            secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
            algorithm = io.jsonwebtoken.SignatureAlgorithm.HS512;
        } else if (keyBytes.length >= HS384_MIN_KEY_LENGTH) {
            // 密钥长度足够支持HS384
            secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
            algorithm = io.jsonwebtoken.SignatureAlgorithm.HS384;
        } else if (keyBytes.length >= HS256_MIN_KEY_LENGTH) {
            // 密钥长度足够支持HS256
            secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
            algorithm = io.jsonwebtoken.SignatureAlgorithm.HS256;
        } else {
            // 密钥长度不足，使用标准方法生成安全密钥
            // 为HS512生成64字节密钥
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-512");
                byte[] hashedBytes = digest.digest(keyBytes);
                // 确保我们有64字节用于HS512，使用完整的哈希结果（通常为64字节）
                secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(hashedBytes);
                algorithm = io.jsonwebtoken.SignatureAlgorithm.HS512;
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate secure key", e);
            }
        }
        secretKeyAndSignatureAlgorithm.secretKey = secretKey;
        secretKeyAndSignatureAlgorithm.algorithm = algorithm;
        return secretKeyAndSignatureAlgorithm;
    }

    /**
     * 验证JWT token的有效性并提取appId
     * 
     * @param token JWT token字符串
     * @return 如果token有效返回对应的appId，否则抛出异常
     * @throws RuntimeException 当token验证失败或算法不可用时抛出此异常
     */
    default String validateAndExtractAppIdFromToken(String token) {
        String defaultSecretKey = getTokenSecret();
        byte[] keyBytes = defaultSecretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 尝试使用不同长度的密钥和算法来验证token
        // 按安全性从高到低尝试
        // 首先尝试使用原始密钥（如果足够长）
        if (keyBytes.length >= 64) {
            // 尝试HS512
            try {
                io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                        .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                return claims.getSubject();
            } catch (Exception e) {
                // 如果失败，继续尝试其他算法
            }
        }

        if (keyBytes.length >= 48) {
            // 尝试HS384
            try {
                io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                        .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                return claims.getSubject();
            } catch (Exception e) {
                // 如果失败，继续尝试其他算法
            }
        }

        if (keyBytes.length >= 32) {
            // 尝试HS256
            try {
                io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                        .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                return claims.getSubject();
            } catch (Exception e) {
                // 如果失败，继续尝试其他方法
            }
        }

        // 如果原始密钥长度不足，尝试使用处理后的密钥
        // 生成不同长度的密钥以匹配不同算法
        try {
            // 缓存常用的MessageDigest实例
            java.security.MessageDigest sha512Digest = java.security.MessageDigest.getInstance("SHA-512");
            java.security.MessageDigest sha384Digest = java.security.MessageDigest.getInstance("SHA-384");
            java.security.MessageDigest sha256Digest = java.security.MessageDigest.getInstance("SHA-256");

            // 为HS512生成64字节密钥
            byte[] hs512KeyBytes = sha512Digest.digest(keyBytes);
            try {
                io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                        .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(hs512KeyBytes))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                return claims.getSubject();
            } catch (Exception e) {
                // 继续尝试其他方式
            }

            // 为HS384生成48字节密钥
            byte[] hs384KeyBytes = sha384Digest.digest(keyBytes);
            try {
                io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                        .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(hs384KeyBytes))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                return claims.getSubject();
            } catch (Exception e) {
                // 继续尝试其他方式
            }

            // 为HS256生成32字节密钥
            byte[] hs256KeyBytes = sha256Digest.digest(keyBytes);
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                    .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(hs256KeyBytes))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to validate token due to missing algorithm", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate token", e);
        }
    }

    /**
     * 秘钥和签名算法封装类
     * 用于存储JWT签名所需的秘钥和对应的签名算法
     */
    @Data
    public static class SecretKeyAndSignatureAlgorithm{
        /**
         * JWT签名使用的秘钥
         */
        private javax.crypto.SecretKey secretKey;
        
        /**
         * JWT签名算法
         */
        private io.jsonwebtoken.SignatureAlgorithm algorithm;
    }


    /**
     * 获取默认的应用ID
     *
     * @return 默认的应用ID，如果配置中未指定则返回默认值"default"
     */
    default String getDefaultKey() {
        return getConfigValueAsString("appId","default");
    }


    /**
     * 获取初始化业务类型
     *
     * @return 初始化业务类型，如果配置中未指定则返回默认值"init"
     */
    default String getInitBussinessType() {
        return getConfigValueAsString("init.type","init");
    }


    /**
     * 获取关闭连接的业务类型
     *
     * @return 关闭连接的业务类型，如果配置中未指定则返回默认值"close"
     */
    default String getCloseBussinessType() {
        return getConfigValueAsString("close.type","close");
    }

    /**
     * 获取SSE连接的超时时间配置值
     *
     * @return 超时时间，单位为毫秒，默认值为 5分钟 = 5 * 60 * 1000 = 300,000 毫秒
     */
    default long getTimeOut() {
        return getConfigValueAsLong("sse.timeount",300000L);
    }

    /**
     * 获取最大客户端会话数
     *
     * @return 最大客户端会话数
     */
    default int getMaxClientSessions() {
        // 默认10000个客户端，可以通过配置进行修改
        return getConfigValueAsInt("session.cache.maxClients", 10000);
    }

    /**
     * 获取会话缓存过期时间（分钟）
     *
     * @return 会话缓存过期时间（分钟）
     */
    default int getSessionCacheExpireMinutes() {
        // 默认30分钟，可以通过配置进行修改
        return getConfigValueAsInt("session.cache.expireMinutes", 30);
    }

    /**
     * 获取缓存并发级别
     *
     * @return 缓存并发级别
     */
    default int getCacheConcurrencyLevel() {
        // 默认并发级别为16，可以通过配置进行修改
        return getConfigValueAsInt("session.cache.concurrencyLevel", 16);
    }


    /**
     * 获取会话延迟时间（毫秒）
     *
     * @return 延迟时间，单位为毫秒，默认值为1000毫秒
     */
    default int getDelayMillis() {
        return getConfigValueAsInt("session.delayMillis", 1000);
    }





}
