package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.BaseDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.MagicAPIHandshakeInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.web.MagicWsController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.SimpleWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsEndpointRegistrar;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * magic 侧端到端接线验收：地基由公共模块提供，magic 只通过 SPI 接入，且行为语义不变。
 *
 * @author zzy
 */
class MagicAPIWsWiringTest {

    /**
     * 32 字节密钥，走 HS256 分支，与 {@code getSecretKeyAndSignatureAlgorithm()} 的选择路径一致。
     */
    private static final String TOKEN_SECRET = "0123456789abcdef0123456789abcdef";

    /**
     * 绑定 /ws/api2 的独立密钥，与 TOKEN_SECRET 内容不同。
     */
    private static final String PATH_BOUND_SECRET = "fedcba9876543210fedcba9876543210";

    private static final String APP_ID = "ws-token-app";

    private static final String APP_SECRET = "ws-token-secret";

    private final MagicAPIProperties properties = new MagicAPIProperties();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class))
            .withUserConfiguration(MagicAPIWsConfiguration.class)
            .withBean(MagicAPIProperties.class, () -> properties)
            .withBean(SpringUtils.class);

    @Test
    void magicShouldOnlySupplyBusinessDispatcher() throws Exception {
        runner.run(context -> {
            assertThat(context).hasSingleBean(WsBusinessDispatcher.class);
            assertThat(context.getBean(WsBusinessDispatcher.class)).isInstanceOf(BaseDynamicWebSocketHandler.class);

            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            Field field = AbstractWebSocketHandler.class.getDeclaredField("businessDispatcher");
            field.setAccessible(true);
            assertThat(field.get(handler)).isSameAs(context.getBean(WsBusinessDispatcher.class));
        });
    }

    @Test
    void foundationHandlerShouldNotBeReplacedByMagic() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AbstractWebSocketHandler.class);
            assertThat(context.getBean(AbstractWebSocketHandler.class)).isInstanceOf(SimpleWebSocketHandler.class);
        });
    }

    @Test
    void magicResourcesAndModuleShouldStillBeRegistered() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(WsDataSourceInfoMagicResourceStorage.class);
            assertThat(context).hasSingleBean(WsSubscribeInfoMagicResourceStorage.class);
            assertThat(context).hasSingleBean(WsDataSourceMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(WsSubscribeMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(WsModule.class);
        });
    }

    @Test
    void endpointPathShouldUseMagicWebBasePath() {
        properties.setWeb("/magic-api");
        runner.run(context -> {
            WebSocketHandlerRegistry registry = mockRegistry();

            context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

            assertThat(registeredPaths(registry)).containsExactly("/magic-api/ws/api");
        });
    }

    /**
     * A1 的 magic 侧证据：追加路径与 {@code magic.api.web} 推导出的基础路径共同展开。
     */
    @Test
    void additionalPathsShouldRegisterAlongsideMagicWebBasePath() {
        properties.setWeb("/magic-api");
        runner.withPropertyValues("fw.plus.ws.websocket.paths=/ws/api2,/ws/api")
                .run(context -> {
                    WebSocketHandlerRegistry registry = mockRegistry();

                    context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

                    assertThat(registeredPaths(registry))
                            .containsExactly("/magic-api/ws/api", "/magic-api/ws/api2");
                });
    }

    @Test
    void magicHandshakeInterceptorShouldBeAttachedToEndpoint() throws Exception {
        properties.setWeb("");
        HandshakeInterceptor interceptor = mock(MagicAPIHandshakeInterceptor.class);
        runner.withBean(MagicAPIHandshakeInterceptor.class, () -> (MagicAPIHandshakeInterceptor) interceptor)
                .run(context -> {
                    WebSocketConfigurer registrar = context.getBean(WebSocketConfigurer.class);
                    Field field = WsEndpointRegistrar.class.getDeclaredField("handshakeInterceptorMap");
                    field.setAccessible(true);
                    Map<String, ?> interceptorMap = (Map<String, ?>) field.get(registrar);

                    assertThat(interceptorMap.values()).containsExactly(interceptor);
                });
    }

    /**
     * jjwt 迁移后 token 语义不变的消费侧证据：{@code WsModule.generateToken} 签发的 token，
     * subject 取数据源 key，过期时间按 {@code fw.plus.ws.token.expireHours} 生效。
     */
    @Test
    void generatedTokenShouldHonorConfiguredAppIdAndExpireHours() {
        runner.withPropertyValues(
                        "fw.plus.ws.token.secret=" + TOKEN_SECRET,
                        "fw.plus.ws.token.expireHours=7")
                .run(context -> {
                    registerDataSource(context, defaultDataSourceInfo());

                    WsModule module = new WsModule(context.getBean(AbstractWebSocketHandler.class), APP_ID);
                    Claims claims = Jwts.parser()
                            .verifyWith(Keys.hmacShaKeyFor(TOKEN_SECRET.getBytes(StandardCharsets.UTF_8)))
                            .build()
                            .parseSignedClaims(module.generateToken(APP_SECRET))
                            .getPayload();

                    assertThat(claims.getSubject()).isEqualTo(APP_ID);
                    assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                            .isEqualTo(TimeUnit.HOURS.toMillis(7L));
                });
    }

    /**
     * 一个实例对应一条路径 + 一个 token：datasource 绑定 /ws/api2 后，
     * {@code generateToken} 用该路径绑定的密钥与有效期签发。
     */
    @Test
    void generatedTokenShouldHonorPathBoundSecret() {
        runner.withPropertyValues(
                        "fw.plus.ws.token.secret=" + TOKEN_SECRET,
                        "fw.plus.ws.tokens[0].path=/ws/api2",
                        "fw.plus.ws.tokens[0].secret=" + PATH_BOUND_SECRET,
                        "fw.plus.ws.tokens[0].expireHours=5")
                .run(context -> {
                    registerDataSource(context, boundDataSourceInfo());

                    WsModule module = new WsModule(context.getBean(AbstractWebSocketHandler.class), APP_ID);
                    String token = module.generateToken(APP_SECRET);

                    Claims claims = Jwts.parser()
                            .verifyWith(Keys.hmacShaKeyFor(PATH_BOUND_SECRET.getBytes(StandardCharsets.UTF_8)))
                            .build()
                            .parseSignedClaims(token)
                            .getPayload();
                    assertThat(claims.getSubject()).isEqualTo(APP_ID);
                    assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                            .isEqualTo(TimeUnit.HOURS.toMillis(5L));

                    try {
                        Jwts.parser()
                                .verifyWith(Keys.hmacShaKeyFor(TOKEN_SECRET.getBytes(StandardCharsets.UTF_8)))
                                .build()
                                .parseSignedClaims(token);
                        Assertions.fail("全局密钥不应验证通过路径绑定密钥签发的 token");
                    } catch (Exception expected) {
                        // 预期：签名不匹配，证明签发用的确实是路径绑定密钥
                    }
                });
    }

    /**
     * 控制台「测试数据源」按 datasource 绑定的路径选密钥：未配置全局密钥时，
     * 绑定了路径的 datasource 仍可测通，未绑定的则明确失败。
     */
    @Test
    void datasourceTestShouldUsePathBoundSecretWhenGlobalSecretMissing() {
        runner.withPropertyValues(
                        "fw.plus.ws.tokens[0].path=/ws/api2",
                        "fw.plus.ws.tokens[0].secret=" + PATH_BOUND_SECRET)
                .run(context -> {
                    AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
                    MagicWsController controller =
                            new MagicWsController(new org.ssssssss.magicapi.core.config.MagicConfiguration(),
                                    null, null, handler);

                    JsonBean<Object> boundResult = controller.test(boundDataSourceInfo());
                    assertThat(boundResult.getCode()).isEqualTo(1);
                    assertThat(boundResult.getData()).isEqualTo("ok");

                    WsDataSourceInfo unbound = defaultDataSourceInfo();
                    unbound.setKey("unbound-app");
                    unbound.setPath("");
                    JsonBean<Object> unboundResult = controller.test(unbound);
                    assertThat(unboundResult.getCode()).isEqualTo(-1);
                    assertThat(unboundResult.getMessage()).contains("fw.plus.ws.token.secret");
                });
    }

    private static void registerDataSource(ApplicationContext context, WsDataSourceInfo dataSourceInfo) {
        List<MagicEntity> dataSources = new ArrayList<>();
        dataSources.add(dataSourceInfo);
        MagicResourceService magicResourceService = mock(MagicResourceService.class);
        when(magicResourceService.files("ws-datasource")).thenReturn(dataSources);
        context.getBean(WsDataSourceInfoMagicResourceStorage.class).setMagicResourceService(magicResourceService);
    }

    private static WsDataSourceInfo defaultDataSourceInfo() {
        WsDataSourceInfo dataSourceInfo = new WsDataSourceInfo();
        dataSourceInfo.setKey(APP_ID);
        dataSourceInfo.setName(APP_ID);
        dataSourceInfo.setSecret(APP_SECRET);
        return dataSourceInfo;
    }

    private static WsDataSourceInfo boundDataSourceInfo() {
        WsDataSourceInfo dataSourceInfo = defaultDataSourceInfo();
        dataSourceInfo.setPath("/ws/api2");
        return dataSourceInfo;
    }

    private static WebSocketHandlerRegistry mockRegistry() {
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        when(registration.setAllowedOrigins(any())).thenReturn(registration);
        when(registry.addHandler(any(), anyString())).thenReturn(registration);
        return registry;
    }

    private static List<String> registeredPaths(WebSocketHandlerRegistry registry) {
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(registry, atLeastOnce()).addHandler(any(), pathCaptor.capture());
        return pathCaptor.getAllValues();
    }
}
