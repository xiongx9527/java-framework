package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth.*;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.service.IMqttService;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.service.impl.MqttServiceImpl;
import net.dreamlu.mica.net.core.ssl.SSLEngineCustomizer;
import net.dreamlu.mica.net.core.ssl.SslConfig;
import net.dreamlu.mica.net.http.common.router.HttpFilter;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.dromara.mica.mqtt.core.server.MqttServerCreator;
import org.dromara.mica.mqtt.core.server.MqttServerCustomizer;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerAuthHandler;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerPublishPermission;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerUniqueIdService;
import org.dromara.mica.mqtt.core.server.event.IMqttConnectStatusListener;
import org.dromara.mica.mqtt.core.server.event.IMqttMessageListener;
import org.dromara.mica.mqtt.core.server.event.IMqttSessionListener;
import org.dromara.mica.mqtt.core.server.interceptor.IMqttMessageInterceptor;
import org.dromara.mica.mqtt.core.server.pipeline.MqttPublishPipelineHandler;
import org.dromara.mica.mqtt.core.server.session.IMqttSessionManager;
import org.dromara.mica.mqtt.core.server.session.InMemoryMqttSessionManager;
import org.dromara.mica.mqtt.core.server.store.IMqttMessageStore;
import org.dromara.mica.mqtt.core.server.store.InMemoryMqttMessageStore;
import org.dromara.mica.mqtt.spring.server.MqttServerTemplate;
import org.dromara.mica.mqtt.spring.server.config.MqttServerLauncher;
import org.dromara.mica.mqtt.spring.server.config.MqttServerProperties;
import org.dromara.mica.mqtt.spring.server.event.SpringEventMqttConnectStatusListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * mqtt server 配置
 *
 * @author L.cm
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({MqttServerProperties.class, FwPlusMqttServerSecurityProperties.class})
public class MqttServerConfiguration {

    /**
     * 创建MQTT服务器唯一ID服务Bean
     * 当容器中不存在IMqttServerUniqueIdService类型的Bean时，创建并返回MqttUniqueIdService实例
     *
     * @return IMqttServerUniqueIdService MQTT服务器唯一ID服务实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IMqttServerUniqueIdService mqttServerUniqueIdService() {
        return new MqttUniqueIdService();
    }

    /**
     * 创建MQTT订阅验证器Bean
     * 当容器中不存在IMqttServerSubscribeValidator类型的Bean时，创建并返回MqttSubscribeValidator实例
     *
     * @return IMqttServerSubscribeValidator MQTT订阅验证器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IMqttServerSubscribeValidator mqttSubscribeValidator(FwPlusMqttServerSecurityProperties securityProperties) {
        return new MqttSubscribeValidator(securityProperties);
    }

    /**
     * 创建MQTT发布权限验证器Bean
     * 当容器中不存在IMqttServerPublishPermission类型的Bean时，创建并返回MqttPublishPermission实例
     *
     * @return IMqttServerPublishPermission MQTT发布权限验证器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IMqttServerPublishPermission mqttPublishPermission(FwPlusMqttServerSecurityProperties securityProperties) {
        return new MqttPublishPermission(securityProperties);
    }

    /**
     * 创建MQTT HTTP认证过滤器Bean
     * 当容器中不存在HttpFilter类型的Bean时，创建并返回MqttHttpAuthFilter实例
     *
     * @return HttpFilter HTTP认证过滤器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpFilter mqttHttpAuthFilter() {
        return new MqttHttpAuthFilter();
    }

    /**
     * 创建MQTT服务器认证处理器Bean
     * 当容器中不存在IMqttServerAuthHandler类型的Bean时，创建并返回MqttAuthHandler实例
     *
     * @return IMqttServerAuthHandler MQTT服务器认证处理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IMqttServerAuthHandler mqttServerAuthHandler(MqttServerProperties properties,
                                                        FwPlusMqttServerSecurityProperties securityProperties) {
        return new MqttAuthHandler(properties, securityProperties);
    }

    /**
     * 创建Spring事件驱动的MQTT连接状态监听器Bean
     * 当容器中不存在该类型Bean时，创建并返回SpringEventMqttConnectStatusListener实例
     * 该监听器用于发布MQTT连接状态变化的Spring事件
     *
     * @param eventPublisher Spring应用事件发布器，用于发布MQTT相关事件
     * @return IMqttConnectStatusListener MQTT连接状态监听器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IMqttConnectStatusListener springEventMqttConnectStatusListener(ApplicationEventPublisher eventPublisher) {
        return new SpringEventMqttConnectStatusListener(eventPublisher);
    }

    /**
     * 创建并配置 MQTT 服务器实例的 Bean。
     * <p>
     * 该方法根据传入的配置属性和各种可选组件（如认证处理器、消息监听器等），
     * 构建一个 MQTT 服务器创建器（MqttServerCreator），用于后续启动 MQTT 服务。
     * 支持的功能包括：协议监听器（MQTT/MQTT-SSL/WebSocket/HTTP API）、
     * 认证机制、会话管理、发布管线（含 Redis 集群转发）、拦截器及自定义扩展等。
     * <p>
     * 注意：mica-mqtt 2.6.x 起 ip/port 由协议监听器承载，
     * 对应配置项为 mica.mqtt.server.mqtt-listener/mqtt-ssl-listener/ws-listener/http-listener。
     *
     * @param properties                      MQTT 服务器的基础配置属性
     * @param authHandlerObjectProvider       可选的认证处理器提供者
     * @param uniqueIdServiceObjectProvider   可选的唯一 ID 服务提供者
     * @param subscribeValidatorObjectProvider 可选的订阅校验器提供者
     * @param publishPermissionObjectProvider 可选的发布权限校验器提供者
     * @param publishPipelineHandlerObjectProvider 可选的发布管线处理器提供者（如 Redis 集群转发）
     * @param messageStoreObjectProvider      可选的消息存储器提供者
     * @param sessionManagerObjectProvider    可选的会话管理器提供者
     * @param sessionListenerObjectProvider   可选的会话监听器提供者
     * @param messageListenerObjectProvider   可选的消息监听器提供者
     * @param connectStatusListenerObjectProvider 可选的连接状态监听器提供者
     * @param messageInterceptorObjectProvider 可选的消息拦截器提供者
     * @param httpFilterObjectProvider       可选的 HTTP 认证过滤器提供者
     * @param sslCustomizers                  可选的 SSL 引擎自定义器提供者
     * @param customizers                     可选的服务器自定义器提供者
     * @return 配置完成的 MqttServerCreator 实例，可用于创建和启动 MQTT 服务器
     */
    @Bean
    @ConditionalOnMissingBean
    public MqttServerCreator mqttServerCreator(MqttServerProperties properties,
                                               ObjectProvider<IMqttServerAuthHandler> authHandlerObjectProvider,
                                               ObjectProvider<IMqttServerUniqueIdService> uniqueIdServiceObjectProvider,
                                               ObjectProvider<IMqttServerSubscribeValidator> subscribeValidatorObjectProvider,
                                               ObjectProvider<IMqttServerPublishPermission> publishPermissionObjectProvider,
                                               ObjectProvider<MqttPublishPipelineHandler> publishPipelineHandlerObjectProvider,
                                               ObjectProvider<IMqttMessageStore> messageStoreObjectProvider,
                                               ObjectProvider<IMqttSessionManager> sessionManagerObjectProvider,
                                               ObjectProvider<IMqttSessionListener> sessionListenerObjectProvider,
                                               ObjectProvider<IMqttMessageListener> messageListenerObjectProvider,
                                               ObjectProvider<IMqttConnectStatusListener> connectStatusListenerObjectProvider,
                                               ObjectProvider<IMqttMessageInterceptor> messageInterceptorObjectProvider,
                                               ObjectProvider<HttpFilter> httpFilterObjectProvider,
                                               ObjectProvider<SSLEngineCustomizer> sslCustomizers,
                                               ObjectProvider<MqttServerCustomizer> customizers) {
        MqttServerCreator serverCreator = MqttServer.create()
                .name(properties.getName())
                .heartbeatTimeout(properties.getHeartbeatTimeout())
                .keepaliveBackoff(properties.getKeepaliveBackoff())
                .readBufferSize((int) properties.getReadBufferSize().toBytes())
                .maxBytesInMessage((int) properties.getMaxBytesInMessage().toBytes())
                .maxClientIdLength(properties.getMaxClientIdLength())
                .nodeName(properties.getNodeName())
                .statEnable(properties.isStatEnable());

        // 启用调试模式
        if (properties.isDebug()) {
            serverCreator.debug();
        }

        // MQTT 协议监听器
        MqttServerProperties.Listener mqttListener = properties.getMqttListener();
        if (mqttListener.isEnable()) {
            serverCreator.enableMqtt(builder -> builder.serverNode(mqttListener.getServerNode()).build());
        }

        // MQTT SSL 协议监听器（自定义 SSLEngine 时应用）
        MqttServerProperties.SslListener mqttSslListener = properties.getMqttSslListener();
        if (mqttSslListener.isEnable()) {
            serverCreator.enableMqttSsl(sslBuilder -> {
                SslConfig sslConfig = createSslConfig(mqttSslListener.getSsl());
                sslCustomizers.ifAvailable(sslConfig::setSslEngineCustomizer);
                return sslBuilder.serverNode(mqttSslListener.getServerNode()).sslConfig(sslConfig).build();
            });
        }

        // WebSocket 协议监听器
        MqttServerProperties.Listener wsListener = properties.getWsListener();
        if (wsListener.isEnable()) {
            serverCreator.enableMqttWs(builder -> builder.serverNode(wsListener.getServerNode()).build());
        }

        // HTTP API 协议监听器：自定义过滤器 / Basic 认证 / SSL
        MqttServerProperties.HttpListener httpListener = properties.getHttpListener();
        if (httpListener.isEnable()) {
            serverCreator.enableMqttHttpApi(builder -> {
                builder.serverNode(httpListener.getServerNode());
                MqttServerProperties.HttpAuth httpAuth = resolveHttpAuth(httpListener);
                if (httpAuth.isEnable()) {
                    httpFilterObjectProvider.ifAvailable(builder::authFilter);
                } else if (httpListener.getBasicAuth().isEnable()) {
                    builder.basicAuth(httpListener.getBasicAuth().getUsername(), httpListener.getBasicAuth().getPassword());
                }
                MqttServerProperties.HttpSsl httpSsl = httpListener.getSsl();
                if (httpSsl.isEnable()) {
                    builder.sslConfig(createSslConfig(httpSsl));
                }
                return builder.build();
            });
        }

        // 设置自定义消息监听器
        messageListenerObjectProvider.ifAvailable(serverCreator::messageListener);

        // 设置认证处理器
        authHandlerObjectProvider.ifAvailable(serverCreator::authHandler);

        // 设置唯一 ID 服务
        uniqueIdServiceObjectProvider.ifAvailable(serverCreator::uniqueIdService);

        // 设置订阅校验器
        subscribeValidatorObjectProvider.ifAvailable(serverCreator::subscribeValidator);

        // 设置发布权限校验器
        publishPermissionObjectProvider.ifAvailable(serverCreator::publishPermission);

        // 注册发布管线处理器（如 Redis 集群转发，晚于本地投递执行）
        publishPipelineHandlerObjectProvider.orderedStream().forEach(serverCreator::addPublishPipelineHandler);

        // 设置消息存储器
        messageStoreObjectProvider.ifAvailable(serverCreator::messageStore);

        // 设置会话管理器
        sessionManagerObjectProvider.ifAvailable(serverCreator::sessionManager);

        // 设置会话监听器
        sessionListenerObjectProvider.ifAvailable(serverCreator::sessionListener);

        // 设置连接状态监听器
        connectStatusListenerObjectProvider.ifAvailable(serverCreator::connectStatusListener);

        // 添加消息拦截器（按顺序）
        messageInterceptorObjectProvider.orderedStream().forEach(serverCreator::addInterceptor);

        // 应用自定义配置
        customizers.ifAvailable((customizer) -> customizer.customize(serverCreator));

        return serverCreator;
    }

    /**
     * 创建用于处理HTTP认证的配置对象
     *
     * @param httpListener HTTP监听器配置对象，包含认证相关的配置信息
     * @return 返回解析后的HTTP认证配置对象
     */
    private static MqttServerProperties.HttpAuth resolveHttpAuth(MqttServerProperties.HttpListener httpListener) {
        MqttServerProperties.HttpAuth auth = httpListener.getAuth();
        return auth == null ? new MqttServerProperties.HttpAuth() : auth;
    }

    /**
     * 根据SSL配置创建服务端SSL配置对象
     *
     * @param ssl SSL配置对象，包含密钥库、信任库等安全相关配置
     * @return 返回构建完成的SslConfig实例
     */
    private static SslConfig createSslConfig(MqttServerProperties.Ssl ssl) {
        return SslConfig.forServer(
                ssl.getKeystorePath(),
                ssl.getKeystorePass(),
                ssl.getTruststorePath(),
                ssl.getTruststorePass(),
                ssl.getClientAuth()
        );
    }

    /**
     * 创建MQTT服务器实例的Bean
     * 该方法依赖于mqttServerCreator Bean来构建MQTT服务器实例
     *
     * @param mqttServerCreator MQTT服务器创建器，用于构建MQTT服务器实例
     * @return 返回构建好的MQTT服务器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn(value={"mqttServerCreator"})
    public MqttServer mqttServer(MqttServerCreator mqttServerCreator) {
        return mqttServerCreator.build();
    }

    /**
     * 创建MQTT服务器启动器的Bean
     * 该方法依赖于已创建的MQTT服务器实例来初始化启动器
     *
     * @param mqttServer 已创建的MQTT服务器实例
     * @return 返回MQTT服务器启动器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn(value={"mqttServer"})
    public MqttServerLauncher mqttServerLauncher(MqttServer mqttServer) {
        return new MqttServerLauncher(mqttServer);
    }

    /**
     * 创建MQTT服务器模板的Bean
     * 该方法依赖于已创建的MQTT服务器实例来创建模板对象
     *
     * @param mqttServer 已创建的MQTT服务器实例
     * @return 返回MQTT服务器模板实例
     */
    @Bean
    @ConditionalOnMissingBean
    @DependsOn(value={"mqttServer"})
    public MqttServerTemplate mqttServerTemplate(MqttServer mqttServer) {
        return new MqttServerTemplate(mqttServer);
    }

    /**
     * 创建MQTT会话管理器Bean实例
     * 当classpath中缺少MicaRedisCache类时创建基于内存的MQTT会话管理器
     *
     * @return IMqttSessionManager MQTT会话管理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IMqttSessionManager mqttSessionManager() {
        return new InMemoryMqttSessionManager();
    }

    /**
     * 创建MQTT消息存储Bean实例
     * 当classpath中缺少MicaRedisCache类时创建基于内存的MQTT消息存储器
     *
     * @return IMqttMessageStore MQTT消息存储器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IMqttMessageStore mqttMessageStore() {
        return new InMemoryMqttMessageStore();
    }

    /**
     * 创建MQTT服务Bean实例
     * 当classpath中缺少MicaRedisCache类时创建MQTT服务实现
     *
     * @param mqttServer MQTT服务器实例，由Spring容器注入
     * @param properties MQTT服务器配置属性（2.6.x 起端口由协议监听器承载，从配置读取）
     * @return IMqttService MQTT服务实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IMqttService mqttService(MqttServer mqttServer, MqttServerProperties properties) {
        return new MqttServiceImpl(mqttServer, resolveMqttPort(properties));
    }

    /**
     * 解析MQTT协议监听端口
     *
     * @param properties MQTT服务器配置属性
     * @return 返回MQTT监听端口；未配置时返回null
     */
    private static Integer resolveMqttPort(MqttServerProperties properties) {
        MqttServerProperties.Listener mqttListener = properties.getMqttListener();
        if (mqttListener == null || mqttListener.getServerNode() == null) {
            return null;
        }
        return mqttListener.getServerNode().getPort();
    }

}
