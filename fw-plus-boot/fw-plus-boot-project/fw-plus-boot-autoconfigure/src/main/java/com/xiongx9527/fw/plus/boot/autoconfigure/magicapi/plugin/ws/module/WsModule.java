package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@MagicModule("ws")
public class WsModule implements DynamicAttribute<WsModule, WsModule> , DynamicModule<WsModule>, BaseService {

    /**
     * WebSocket处理器
     */
    private final AbstractWebSocketHandler webSocketHandler;

    /**
     * 数据源键名
     */
    private final String key;

    /**
     * WebSocket模块构造函数
     *
     * @param webSocketHandler WebSocket处理器
     * @param key 数据源键名
     */
    public WsModule(AbstractWebSocketHandler webSocketHandler, String key) {
        this.webSocketHandler = webSocketHandler;
        this.key = key;
    }

    /**
     * 向指定客户端广播消息
     *
     * @param clientId 客户端ID
     * @param message 要发送的消息内容
     */
    public void broadcastMessageToClient(String clientId , String message) {
        this.webSocketHandler.broadcastMessageToClient(key, clientId, message);
    }

    /**
     * 向所有客户端广播分布式消息
     *
     * @param message 要广播的消息内容
     */
    public void broadcastDistributedMessageToAllClient(String message) {
        this.webSocketHandler.broadcastDistributedMessageToAllClient(key, message);
    }

    /**
     * 生成JWT Token
     *
     * @param secret 应用密钥
     * @return 生成的JWT Token字符串
     * @throws SecurityException 当appId或密钥无效时抛出此异常
     */
    public String generateToken(String secret) {
        String appId = key ;
        // 校验appId和密钥是否正确
        try {
            List<WsDataSourceInfo> dataSourceInfos = SpringUtils.getBean(WsDataSourceInfoMagicResourceStorage.class).findAll();
            WsDataSourceInfo dataSourceInfo = ListUtils.emptyIfNull(dataSourceInfos).stream()
                    .filter(info -> {
                        String mySecret = MagicAPIConfigUtils.getWsConfig(info.getKey(), "secret", info.getSecret());
                        return info.getKey().equals(appId) && java.util.Objects.equals(mySecret, secret);
                    })
                    .findFirst().orElse(null);
            if (Objects.isNull(dataSourceInfo)) {
                throw new SecurityException("Invalid appId or secret");
            }

            // 从配置中获取过期时间（默认2小时）
            long expireHours = getTokenExpireHours();

            Date now = new Date();
            // 设置过期时间，使用TimeUnit提高可读性并避免整数溢出
            long expireMillis = now.getTime() + java.util.concurrent.TimeUnit.HOURS.toMillis(expireHours);
            Date expireDate = new Date(expireMillis);

            SecretKeyAndSignatureAlgorithm secretKeyAndSignatureAlgorithm = getSecretKeyAndSignatureAlgorithm();
            if (secretKeyAndSignatureAlgorithm == null) {
                throw new RuntimeException("Failed to obtain secret key and signature algorithm");
            }
            javax.crypto.SecretKey secretKey = secretKeyAndSignatureAlgorithm.getSecretKey();
            io.jsonwebtoken.SignatureAlgorithm algorithm = secretKeyAndSignatureAlgorithm.getAlgorithm();

            io.jsonwebtoken.JwtBuilder builder = io.jsonwebtoken.Jwts.builder()
                    .setSubject(appId)
                    .setIssuedAt(now)
                    .setExpiration(expireDate)
                    .signWith(algorithm, secretKey);
            return builder.compact();
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate token", e);
            throw new RuntimeException("Failed to generate token", e);
        }

    }

    /**
     * 获取动态WebSocket模块
     *
     * @param context MagicScript上下文对象
     * @return WebSocket模块实例，如果未指定数据源则返回当前实例，否则返回新的WsModule实例
     */
    @Override
    public WsModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        return new WsModule(this.webSocketHandler,dataSourceKey);
    }

    /**
     * 根据键名获取动态属性
     *
     * @param key 属性键名，用作数据源键
     * @return 新的WebSocket模块实例
     */
    @Override
    public WsModule getDynamicAttribute(String key) {
        return new WsModule(this.webSocketHandler,key);
    }

}
