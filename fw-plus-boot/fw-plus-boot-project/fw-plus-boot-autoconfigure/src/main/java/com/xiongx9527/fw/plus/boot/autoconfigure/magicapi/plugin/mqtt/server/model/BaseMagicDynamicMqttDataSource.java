package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDownReceiver;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageExchangeReceiver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ssl.ClientAuth;
import net.dreamlu.mica.net.core.ssl.SslConfig;
import net.dreamlu.mica.net.http.common.router.HttpFilter;
import org.apache.commons.lang3.StringUtils;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.dromara.mica.mqtt.core.server.MqttServerCreator;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerAuthHandler;
import org.dromara.mica.mqtt.core.server.pipeline.MqttPublishPipelineHandler;
import org.dromara.mica.mqtt.core.server.session.IMqttSessionManager;
import org.dromara.mica.mqtt.core.server.store.IMqttMessageStore;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class BaseMagicDynamicMqttDataSource implements BaseService, DisposableBean {

    /**
     * 服务器实例集合，用于存储所有已创建的 MQTT 服务器实例。
     */
    private final Map<String, MagicAPIMqttServer> serverMap = new ConcurrentHashMap<>();

    /**
     * 存储数据源名称与对应 MQTT 数据源信息的映射关系。
     * 使用线程安全的 ConcurrentHashMap 实现并发访问控制。
     */
    private final Map<String, MqttDataSourceInfo> infoMap = new ConcurrentHashMap<>();


    /**
     * 获取MQTT服务器唯一ID服务实例
     * @param key 用于标识MQTT服务器的键值
     * @return MagicAPIMqttServerUniqueIdService MQTT服务器唯一ID服务实例
     */
    public abstract MagicAPIMqttServerUniqueIdService getMagicAPIMqttServerUniqueIdService(String key);


    /**
     * 获取MQTT连接状态监听器实例
     * @param key 用于标识连接的键值
     * @return MagicAPIMqttConnectStatusListener MQTT连接状态监听器实例
     */
    public abstract MagicAPIMqttConnectStatusListener getMagicAPIMqttConnectStatusListener(String key);


    /**
     * 获取MQTT消息交换接收器
     *
     * @param mqttServer MQTT服务器实例
     * @param key 用于标识接收器的键值
     * @return RedisMqttMessageDownReceiver MQTT消息下行接收器实例
     */
    public abstract RedisMqttMessageDownReceiver getMqttMessageExchangeReceiver(MqttServer mqttServer,String key);

    /**
     * 获取MQTT消息下行接收器
     *
     * @param mqttServer MQTT服务器实例
     * @param key 用于标识接收器的键值
     * @return RedisMqttMessageExchangeReceiver MQTT消息交换接收器实例
     */
    public abstract RedisMqttMessageExchangeReceiver getMqttMessageDownReceiver(MqttServer mqttServer,String key);

    /**
     * 获取MQTT服务器认证处理器
     *
     * @param key 认证处理器的标识键
     * @return 返回与指定键关联的MQTT服务器认证处理器实例
     */
    public abstract IMqttServerAuthHandler getIMqttServerAuthHandler(String key);

    /**
     * 获取MQTT发布管线处理器实例。
     *
     * <p>mica-mqtt 2.6.x 起消息分发改为发布管线，集群模式通过该处理器把上行消息
     * 转发到 Redis 等外部通道（本地投递由内建管线处理器完成）；单机模式返回 null。</p>
     *
     * @param key 用于标识和获取特定处理器的键值
     * @return 返回与指定键关联的MqttPublishPipelineHandler实例
     */
    public abstract MqttPublishPipelineHandler getMqttPublishPipelineHandler(String key);

    /**
     * 获取指定键对应的MQTT会话管理器实例
     *
     * @param key 用于标识和获取特定MQTT会话管理器的键值
     * @return 返回与指定键关联的IMqttSessionManager实例
     */
    public abstract IMqttSessionManager getIMqttSessionManager(String key);

    /**
     * 获取指定键对应的MQTT消息存储实例
     *
     * @param key 用于标识和获取特定MQTT消息存储的键值
     * @return 返回与指定键关联的IMqttMessageStore实例
     */
    public abstract IMqttMessageStore getIMqttMessageStore(String key);

    /**
     * 构建MQTT服务器实例
     *
     * <p>mica-mqtt 2.6.x 起不再通过反射拼装内部组件，IP/端口由协议监听器承载：
     * MQTT（或 MQTT SSL）、WebSocket、HTTP API 均为独立的协议监听器。</p>
     *
     * @param info MQTT 数据源描述对象
     * @return 构建完成的MagicAPIMqttServer实例
     */
    public MagicAPIMqttServer buildServer(MqttDataSourceInfo info) {
        String key = info.getKey();
        String ip = MagicAPIConfigUtils.getMqttConfig(key, "ip", info.getIp());
        String port = MagicAPIConfigUtils.getMqttConfig(key, "port", info.getPort());
        String webPort = MagicAPIConfigUtils.getMqttConfig(key, "webPort", info.getWebPort());
        int mqttPort = parseInt(port, 1883);
        int webPortInt = parseInt(webPort, 8083);

        String configuredProperties = MagicAPIConfigUtils.getMqttConfig(key, "properties", null);
        Map<String, Object> properties = MagicAPIConfigUtils.mergeProperties(info.getProperties(), configuredProperties);

        long heartbeatTimeout = getConfigValueAsLong(properties, "heartbeatTimeout", getHeartbeatTimeout());
        float keepaliveBackoff = getConfigValueAsFloat(properties, "keepaliveBackoff", getKeepaliveBackoff());
        int maxClientIdLength = getConfigValueAsInt(properties, "maxClientIdLength", getMaxClientIdLength());
        long readBufferSize = getConfigValueAsLong(properties, "readBufferSize", getReadBufferSize());
        long maxBytesInMessage = getConfigValueAsLong(properties, "maxBytesInMessage", getMaxBytesInMessage());
        boolean websocketEnable = getConfigValueAsBoolean(properties, "websocketEnable", false);
        boolean httpEnable = getConfigValueAsBoolean(properties, "httpEnable", false);
        boolean statEnable = getConfigValueAsBoolean(properties, "statEnable", true);
        boolean debug = getConfigValueAsBoolean(properties, "debug", false);

        // 构建基础MQTT服务器配置
        MqttServerCreator serverCreator = MqttServer.create()
                .name(info.getName())
                .heartbeatTimeout(heartbeatTimeout)
                .keepaliveBackoff(keepaliveBackoff)
                .readBufferSize((int) readBufferSize)
                .maxBytesInMessage((int) maxBytesInMessage)
                .maxClientIdLength(maxClientIdLength)
                .nodeName(key)
                .statEnable(statEnable);

        // 启用调试模式（如果配置中开启）
        if (debug) {
            serverCreator.debug();
        }

        // MQTT 协议监听器（SSL 启用时使用 MQTT SSL 监听器）
        SslConfig sslConfig = resolveSslConfig(properties);
        if (sslConfig != null) {
            serverCreator.enableMqttSsl(builder -> builder.serverNode(ip, mqttPort).sslConfig(sslConfig).build());
        } else {
            serverCreator.enableMqtt(builder -> builder.serverNode(ip, mqttPort).build());
        }

        // WebSocket 与 HTTP API 在 mica-mqtt 2.6.x 起为相互独立的协议监听器，不能共用端口
        int wsPort = parseInt(getConfigValueAsString(properties, "wsPort", null), webPortInt);
        int httpPort = parseInt(getConfigValueAsString(properties, "httpPort", null), webPortInt);
        if (websocketEnable && httpEnable && wsPort == httpPort) {
            httpPort = wsPort + 1;
            log.warn("MQTT 数据源[{}] WebSocket 与 HTTP API 在 mica-mqtt 2.6.x 起需独立端口，HTTP API 端口已自动调整为 {}，可通过 properties.httpPort 显式配置", key, httpPort);
        }
        final int finalWsPort = wsPort;
        final int finalHttpPort = httpPort;

        // WebSocket 协议监听器
        if (websocketEnable) {
            serverCreator.enableMqttWs(builder -> builder.serverNode(ip, finalWsPort).build());
        }

        // HTTP API 协议监听器：Basic 认证 + 自定义认证过滤器
        if (httpEnable) {
            Object httpBasicAuthObj = properties.get("httpBasicAuth");
            final Map<String, Object> httpBasicAuthMap = httpBasicAuthObj instanceof Map ? (Map<String, Object>) httpBasicAuthObj : null;
            serverCreator.enableMqttHttpApi(builder -> {
                builder.serverNode(ip, finalHttpPort);
                if (httpBasicAuthMap != null && getConfigValueAsBoolean(httpBasicAuthMap, "enable", false)) {
                    builder.basicAuth(
                            getConfigValueAsString(httpBasicAuthMap, "username", ""),
                            getConfigValueAsString(httpBasicAuthMap, "password", ""));
                }
                HttpFilter httpFilter = getHttpFilter();
                if (httpFilter != null) {
                    builder.authFilter(httpFilter);
                }
                return builder.build();
            });
        }

        // 认证：数据源用户名/密码优先，存在自定义认证处理器时由自定义认证处理器接管
        String username = MagicAPIConfigUtils.getMqttConfig(key, "username", info.getUsername());
        String password = MagicAPIConfigUtils.getMqttConfig(key, "password", info.getPassword());
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            serverCreator.usernamePassword(username, password);
        }
        IMqttServerAuthHandler authHandler = getIMqttServerAuthHandler(key);
        if (authHandler != null) {
            serverCreator.authHandler(authHandler);
        }

        serverCreator.messageListener(new MagicAPIMqttMessageListener(key));
        serverCreator.sessionManager(getIMqttSessionManager(key));
        serverCreator.messageStore(getIMqttMessageStore(key));
        // 集群转发：注册为发布管线处理器（本地投递完成后转发到 Redis）
        MqttPublishPipelineHandler publishPipelineHandler = getMqttPublishPipelineHandler(key);
        if (publishPipelineHandler != null) {
            serverCreator.addPublishPipelineHandler(publishPipelineHandler);
        }
        serverCreator.uniqueIdService(getMagicAPIMqttServerUniqueIdService(key));
        serverCreator.connectStatusListener(getMagicAPIMqttConnectStatusListener(key));

        // 构建最终的MQTT服务器实例并返回
        MqttServer mqttServer = serverCreator.build();
        MagicAPIMqttServer magicAPIMqttServer = new MagicAPIMqttServer(mqttServer,
                getMqttMessageExchangeReceiver(mqttServer, key),
                getMqttMessageDownReceiver(mqttServer, key));
        return magicAPIMqttServer;
    }

    /**
     * 解析SSL配置
     *
     * @param properties 数据源扩展配置
     * @return 启用 SSL 时返回服务端 SslConfig，否则返回 null
     */
    @SuppressWarnings("unchecked")
    private SslConfig resolveSslConfig(Map<String, Object> properties) {
        Object sslObj = properties.get("ssl");
        if (!(sslObj instanceof Map)) {
            return null;
        }
        Map<String, Object> sslMap = (Map<String, Object>) sslObj;
        if (!getConfigValueAsBoolean(sslMap, "enabled", false)) {
            return null;
        }
        ClientAuth clientAuth = ClientAuth.NONE;
        String clientAuthValue = getConfigValueAsString(sslMap, "clientAuth", null);
        if ("OPTIONAL".equalsIgnoreCase(clientAuthValue)) {
            clientAuth = ClientAuth.OPTIONAL;
        } else if ("REQUIRE".equalsIgnoreCase(clientAuthValue)) {
            clientAuth = ClientAuth.REQUIRE;
        }
        return SslConfig.forServer(
                getConfigValueAsString(sslMap, "keystorePath", ""),
                getConfigValueAsString(sslMap, "keystorePass", ""),
                getConfigValueAsString(sslMap, "truststorePath", ""),
                getConfigValueAsString(sslMap, "truststorePass", ""),
                clientAuth
        );
    }

    /**
     * 获取框架提供的HTTP认证过滤器（可选）
     *
     * @return 返回 HttpFilter 实例，未提供时返回 null
     */
    private HttpFilter getHttpFilter() {
        try {
            return SpringUtils.getBean(HttpFilter.class);
        } catch (Exception e) {
            // 忽略：未提供自定义 http 过滤器
        }
        return null;
    }

    /**
     * 字符串转int，解析失败时返回默认值
     *
     * @param value 待解析的字符串
     * @param defaultValue 默认值
     * @return 解析结果或默认值
     */
    private static int parseInt(String value, int defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * 创建MQTT服务器实例
     * @param info
     */
    public void createServer(MqttDataSourceInfo info) {
        MagicAPIMqttServer server = buildServer(info);
        server.start();
        // 将客户端实例按key存储到客户端映射中
        this.serverMap.put(info.getKey(), server);
        // 将数据源信息按key存储到信息映射中
        this.infoMap.put(info.getKey(), info);
    }


    /**
     *
     * @param key
     * @return
     */
    public MagicAPIMqttServer getServer(String key) {
        return this.serverMap.get(key);
    }


    /**
     * 销毁指定的MQTT服务器实例
     * @param server
     */
    public void destroyServer(MagicAPIMqttServer server) {
        if (server != null) {
            try {
                // 停止MQTT服务器
                server.stop();
            } catch (Exception e) {
                log.error("Failed to stop MQTT server", e);
            }
        }
    }



    /**
     * 获取所有数据源节点信息
     *
     * @return 包含所有动态创建的数据源信息以及默认数据源信息的列表
     */
    public List<MqttDataSourceInfo> datasourceNodes() {
        // 获取所有动态创建的数据源信息
        List<MqttDataSourceInfo>  dataSourceInfoList = new ArrayList<>(infoMap.values());
        try {
            // 尝试获取默认数据源客户端，如果存在则添加默认数据源信息
            MqttServer client = SpringUtils.getBean(MqttServer.class);
            if (client != null) {
                MqttDataSourceInfo dataSourceInfo = new MqttDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据指定的键销毁MQTT服务器实例
     *
     * @param key 服务器的唯一标识键，用于查找和销毁对应的服务器实例
     */
    public void destroyServerByKey(String  key) {
        // 参数校验，空key直接返回
        if (key == null) {
            return;
        }
        try {
            // 查找并获取指定键对应的客户端实例
            MagicAPIMqttServer client = this.serverMap.get(key);
            if (client != null) {
                try {
                    // 停止并销毁指定的MQTT服务器实例
                    destroyServer(client);
                } catch (Exception e) {
                    log.error("Failed to stop MQTT server", e);
                }
                // 从映射表中移除服务器引用
                this.serverMap.remove(key);
            }
            // 同时清理相关的配置信息
            this.infoMap.remove(key);
        } catch (Exception e) {
            log.warn("销毁数据源服务器失败: {}", e.getMessage(), e);
        }
    }


    /**
     * 销毁所有MQTT服务器实例
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        // 遍历并销毁所有客户端实例
        for (String key : new ArrayList<>(this.serverMap.keySet())) {
            try {
                destroyServerByKey(key);
            } catch (Exception e) {
                log.warn("销毁数据源客户端失败: {}", e.getMessage(), e);
            }
        }


        try {
            this.serverMap.clear();
            this.infoMap.clear();
        } catch (Exception e) {
            log.error("Failed to destroy MQTT server", e);
        }

    }


}
