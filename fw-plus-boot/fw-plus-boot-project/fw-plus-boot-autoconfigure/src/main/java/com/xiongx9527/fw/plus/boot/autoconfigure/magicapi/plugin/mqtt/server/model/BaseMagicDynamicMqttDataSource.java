package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDownReceiver;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageExchangeReceiver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.MqttServerAioHandler;
import net.dreamlu.iot.mqtt.core.server.MqttServerAioListener;
import net.dreamlu.iot.mqtt.core.server.MqttServerCreator;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerAuthHandler;
import net.dreamlu.iot.mqtt.core.server.broker.DefaultMqttBrokerDispatcher;
import net.dreamlu.iot.mqtt.core.server.cluster.MqttClusterMessageListener;
import net.dreamlu.iot.mqtt.core.server.dispatcher.AbstractMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.http.core.MqttWebServer;
import net.dreamlu.iot.mqtt.core.server.session.IMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.session.InMemoryMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.store.IMqttMessageStore;
import net.dreamlu.iot.mqtt.core.server.store.InMemoryMqttMessageStore;
import net.dreamlu.iot.mqtt.core.server.support.DefaultMqttConnectStatusListener;
import net.dreamlu.iot.mqtt.core.server.support.DefaultMqttServerProcessor;
import net.dreamlu.iot.mqtt.core.server.support.DefaultMqttServerUniqueIdServiceImpl;
import net.dreamlu.iot.mqtt.spring.server.config.MqttServerProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.unit.DataSize;
import org.tio.core.ssl.ClientAuth;
import org.tio.core.ssl.SslConfig;
import org.tio.core.task.HeartbeatMode;
import org.tio.server.TioServer;
import org.tio.server.TioServerConfig;
import org.tio.server.intf.TioServerHandler;
import org.tio.server.intf.TioServerListener;
import org.tio.utils.buffer.ByteBufferAllocator;
import org.tio.utils.hutool.StrUtil;
import org.tio.utils.thread.ThreadUtils;
import org.tio.utils.timer.DefaultTimerTaskService;
import org.tio.utils.timer.TimerTaskService;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
     * 根据提供的 MQTT 数据源信息构建对应的服务器属性配置对象。
     *
     * @param info 包含 MQTT 配置信息的数据源描述对象
     * @return 初始化完成的 MqttServerProperties 对象
     */
    private MqttServerProperties buildMqttServerProperties(MqttDataSourceInfo info) {
        MqttServerProperties mqttServerProperties = new MqttServerProperties();
        String name = info.getName();
        String ip = info.getIp();
        ip = MagicAPIConfigUtils.getMqttConfig(info.getKey(), "ip", ip);
        String port = info.getPort();
        port = MagicAPIConfigUtils.getMqttConfig(info.getKey(), "port", port);
        String webPort = info.getWebPort();
        webPort = MagicAPIConfigUtils.getMqttConfig(info.getKey(), "webPort", webPort);
        // 基础网络配置
        mqttServerProperties.setName(info.getName());
        mqttServerProperties.setIp(ip);
        try {
            mqttServerProperties.setPort(Integer.parseInt(port));
        } catch (Exception e) {
            mqttServerProperties.setPort(1883);
        }

        mqttServerProperties.setNodeName(info.getKey());
        // 端口配置
        mqttServerProperties.setEnabled(true);
        try {
            mqttServerProperties.setWebPort(Integer.parseInt(webPort));
        } catch (Exception e) {
            mqttServerProperties.setWebPort(8083);
        }
        String username = info.getUsername();
        username = MagicAPIConfigUtils.getMqttConfig(info.getKey(), "username", username);
        String password = info.getPassword();
        password = MagicAPIConfigUtils.getMqttConfig(info.getKey(), "password", password);
        // 如果用户名和密码都不为空，则配置MQTT认证信息
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            MqttServerProperties.MqttAuth mqttAuth = new MqttServerProperties.MqttAuth();
            mqttAuth.setEnable(true);
            mqttAuth.setUsername(username);
            mqttAuth.setPassword(password);
            mqttServerProperties.setAuth(mqttAuth);
        }
        String configuredProperties = MagicAPIConfigUtils.getMqttConfig(info.getKey(), "properties", null);
        Map<String, Object> properties = MagicAPIConfigUtils.mergeProperties(info.getProperties(), configuredProperties);
        if (!properties.isEmpty()) {
            try {
                mqttServerProperties.setHeartbeatTimeout(getConfigValueAsLong(properties, "heartbeatTimeout", getHeartbeatTimeout()));
                mqttServerProperties.setKeepaliveBackoff(getConfigValueAsFloat(properties, "keepaliveBackoff", getKeepaliveBackoff()));
                mqttServerProperties.setMaxClientIdLength(getConfigValueAsInt(properties, "maxClientIdLength", getMaxClientIdLength()));

                // 缓冲区配置 - 从 otherConfig 中获取
                long readBufferSize = getConfigValueAsLong(properties, "readBufferSize", getReadBufferSize());
                mqttServerProperties.setReadBufferSize(DataSize.ofBytes(readBufferSize));
                long maxBytesInMessage = getConfigValueAsLong(properties, "maxBytesInMessage", getMaxBytesInMessage());
                mqttServerProperties.setMaxBytesInMessage(DataSize.ofBytes(maxBytesInMessage));

                String bufferAllocator = (String) properties.getOrDefault("bufferAllocator", "");
                if (StringUtils.isNotBlank(bufferAllocator)) {
                    if ("HEAP".equalsIgnoreCase(bufferAllocator)) {
                        mqttServerProperties.setBufferAllocator(ByteBufferAllocator.HEAP);
                    } else if ("DIRECT".equalsIgnoreCase(bufferAllocator)) {
                        mqttServerProperties.setBufferAllocator(ByteBufferAllocator.DIRECT);
                    }
                }

                // 协议支持标志 - 从 otherConfig 中获取
                if (properties.containsKey("websocketEnable")) {
                    mqttServerProperties.setWebsocketEnable((Boolean) properties.get("websocketEnable"));
                }

                if (properties.containsKey("httpEnable")) {
                    mqttServerProperties.setHttpEnable((Boolean) properties.get("httpEnable"));
                }

                if (properties.containsKey("statEnable")) {
                    mqttServerProperties.setStatEnable((Boolean) properties.get("statEnable"));
                }

                if (properties.containsKey("debug")) {
                    mqttServerProperties.setDebug((Boolean) properties.get("debug"));
                }

                // 解析 HTTP Basic 认证配置
                if (properties.containsKey("httpBasicAuth")) {
                    Map<String, Object> httpBasicAuthMap = (Map<String, Object>) properties.get("httpBasicAuth");
                    MqttServerProperties.HttpBasicAuth httpBasicAuth = new MqttServerProperties.HttpBasicAuth();
                    httpBasicAuth.setEnable((Boolean) httpBasicAuthMap.getOrDefault("enable", false));
                    httpBasicAuth.setUsername((String) httpBasicAuthMap.getOrDefault("username", ""));
                    httpBasicAuth.setPassword((String) httpBasicAuthMap.getOrDefault("password", ""));
                    mqttServerProperties.setHttpBasicAuth(httpBasicAuth);
                }

                // 解析 SSL 配置
                if (properties.containsKey("ssl")) {
                    Map<String, Object> sslMap = (Map<String, Object>) properties.get("ssl");
                    MqttServerProperties.Ssl ssl = new MqttServerProperties.Ssl();
                    ssl.setEnabled((Boolean) sslMap.getOrDefault("enabled", false));
                    ssl.setKeystorePath((String) sslMap.getOrDefault("keystorePath", ""));
                    ssl.setKeystorePass((String) sslMap.getOrDefault("keystorePass", ""));
                    ssl.setTruststorePath((String) sslMap.getOrDefault("truststorePath", ""));
                    ssl.setTruststorePass((String) sslMap.getOrDefault("truststorePass", ""));
                    String clientAuth = (String) sslMap.getOrDefault("clientAuth", "");
                    if (StringUtils.isNotBlank(clientAuth)) {
                        if ("NONE".equalsIgnoreCase(clientAuth)) {
                            ssl.setClientAuth(ClientAuth.NONE);
                        } else if ("OPTIONAL".equalsIgnoreCase(clientAuth)) {
                            ssl.setClientAuth(ClientAuth.OPTIONAL);
                        } else if ("REQUIRE".equalsIgnoreCase(clientAuth)) {
                            ssl.setClientAuth(ClientAuth.REQUIRE);
                        }
                    }
                    mqttServerProperties.setSsl(ssl);
                }
            } catch (Exception e) {
                log.warn("应用 MQTT properties 失败: {}", e.getMessage(), e);
            }
        }

        return mqttServerProperties;
    }


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
     * 获取MQTT集群消息监听器实例
     * @param mqttServer MQTT服务器实例
     * @return MqttClusterMessageListener MQTT集群消息监听器实例
     */
    public abstract MqttClusterMessageListener getMqttClusterMessageListener(MqttServer mqttServer);

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
     * 获取MQTT消息分发器实例
     *
     * @param key 用于标识和获取特定MQTT消息分发器的键值
     * @return 返回与指定键关联的MQTT消息分发器实例
     */
    public abstract IMqttMessageDispatcher getIMqttMessageDispatcher(String key);

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
     * @param info
     * @return
     */
    public MagicAPIMqttServer buildServer(MqttDataSourceInfo info) {
        MqttServerProperties properties = buildMqttServerProperties(info);

        // 构建基础MQTT服务器配置
        MqttServerCreator serverCreator = MqttServer.create()
                .name(properties.getName())
                .ip(properties.getIp())
                .port(properties.getPort())
                .heartbeatTimeout(properties.getHeartbeatTimeout())
                .keepaliveBackoff(properties.getKeepaliveBackoff())
                .readBufferSize((int) properties.getReadBufferSize().toBytes())
                .maxBytesInMessage((int) properties.getMaxBytesInMessage().toBytes())
                .bufferAllocator(properties.getBufferAllocator())
                .maxClientIdLength(properties.getMaxClientIdLength())
                .webPort(properties.getWebPort())
                .websocketEnable(properties.isWebsocketEnable())
                .httpEnable(properties.isHttpEnable())
                .nodeName(properties.getNodeName())
                .statEnable(properties.isStatEnable());

        // 启用调试模式（如果配置中开启）
        if (properties.isDebug()) {
            serverCreator.debug();
        }

        // 配置HTTP Basic认证（如果启用）
        MqttServerProperties.HttpBasicAuth httpBasicAuth = properties.getHttpBasicAuth();
        if (serverCreator.isHttpEnable() && httpBasicAuth.isEnable()) {
            serverCreator.httpBasicAuth(httpBasicAuth.getUsername(), httpBasicAuth.getPassword());
        }

        // 配置SSL（如果启用）
        MqttServerProperties.Ssl ssl = properties.getSsl();
        if (ssl.isEnabled()) {
            SslConfig sslConfig = SslConfig.forServer(
                    ssl.getKeystorePath(),
                    ssl.getKeystorePass(),
                    ssl.getTruststorePath(),
                    ssl.getTruststorePass(),
                    ssl.getClientAuth()
            );
            serverCreator.sslConfig(sslConfig);
        }
        serverCreator.messageListener(new MagicAPIMqttMessageListener(info.getKey()));
        serverCreator.sessionManager(getIMqttSessionManager(info.getKey()));
        serverCreator.messageStore(getIMqttMessageStore(info.getKey()));
        serverCreator.messageDispatcher(getIMqttMessageDispatcher(info.getKey()));
        serverCreator.uniqueIdService(getMagicAPIMqttServerUniqueIdService(info.getKey()));
        serverCreator.connectStatusListener(getMagicAPIMqttConnectStatusListener(info.getKey()));
        IMqttServerAuthHandler authHandlerObjectProvider = getIMqttServerAuthHandler(info.getKey());
        if (authHandlerObjectProvider != null) {
            serverCreator.authHandler(authHandlerObjectProvider);
        }
        // 构建最终的MQTT服务器实例并返回
        MqttServer mqttServer = buildMqttServer(serverCreator);
        MagicAPIMqttServer magicAPIMqttServer = new MagicAPIMqttServer(mqttServer,
                getMqttMessageExchangeReceiver(mqttServer,info.getKey()),
                getMqttMessageDownReceiver(mqttServer,info.getKey()));
        return magicAPIMqttServer;
    }

    /**
     * 构建MqttServer实例
     * <p>
     * 通过反射方式创建MqttServer对象实例。当反射创建失败时，会回退使用serverCreator.build()方法创建实例。
     *
     * @param tioServer     Tio服务器实例，用于网络通信
     * @param webServer     MQTT Web服务器实例，提供Web相关功能
     * @param serverCreator MQTT服务器创建器，用于构建服务器实例
     * @param taskService   定时任务服务，用于处理定时任务
     * @return 返回创建的MqttServer实例，如果反射创建失败则返回serverCreator.build()创建的实例
     */
    private MqttServer buildMqttServerInstance(TioServer tioServer,MqttWebServer webServer,MqttServerCreator serverCreator,TimerTaskService taskService) {
        try {
            // 获取 MqttServer 类
            Class<?> mqttServerClass = Class.forName("net.dreamlu.iot.mqtt.core.server.MqttServer");
            // 获取构造函数
            Constructor<?> constructor = mqttServerClass.getDeclaredConstructor(
                    TioServer.class,
                    MqttWebServer.class,
                    MqttServerCreator.class,
                    TimerTaskService.class
            );
            // 设置可访问（如果是包私有或私有构造函数）
            constructor.setAccessible(true);
            // 创建实例
            return (MqttServer)constructor.newInstance(tioServer, webServer, serverCreator, taskService);
        } catch (ClassNotFoundException e) {
            // 处理类未找到异常
            log.error("MqttServer class not found", e);
        } catch (NoSuchMethodException e) {
            // 处理构造函数不存在异常
            log.error("MqttServer constructor not found", e);
        } catch (InstantiationException e) {
            // 处理实例化异常
            log.error("Failed to instantiate MqttServer", e);
        } catch (IllegalAccessException e) {
            // 处理访问权限异常
            log.error("Unable to access MqttServer constructor", e);
        } catch (InvocationTargetException e) {
            // 处理构造函数执行异常
            log.error("Error occurred while creating MqttServer instance", e);
        } catch (Exception e) {
            // 处理其他未预期的异常
            log.error("Unexpected error while building MqttServer", e);
        }
        return serverCreator.build();
    }


    /**
     * 根据传入的 MQTT 服务器创建器（MqttServerCreator）构建一个 MQTT 服务器实例。
     * <p>
     * 该方法会完成如下主要工作：
     * <ul>
     *   <li>设置默认节点名称、服务组件和执行器</li>
     *   <li>初始化定时任务服务</li>
     *   <li>构建处理器和服务监听器</li>
     *   <li>配置 TioServerConfig 相关参数</li>
     *   <li>根据配置决定是否启用 HTTP 和 WebSocket 支持</li>
     *   <li>最终调用重载方法完成 MqttServer 实例的创建</li>
     * </ul>
     *
     * @param serverCreator 用于配置和创建 MQTT 服务器的创建器对象，不能为空
     * @return 构建完成的 MqttServer 实例
     * @throws IllegalArgumentException 当 serverName 为空时抛出
     */
    private MqttServer buildMqttServer(MqttServerCreator serverCreator) {
        // 设置默认节点名称
        if (StrUtil.isBlank(serverCreator.getNodeName())) {
            String pidAndPort = ManagementFactory.getRuntimeMXBean().getName() + ':' + serverCreator.getPort();
            serverCreator.nodeName(pidAndPort);
        }

        // 设置默认服务组件
        setDefaultComponentIfAbsent(serverCreator);

        // 创建定时任务服务
        TimerTaskService taskService = new DefaultTimerTaskService(getTickMs(), getWheelSize());
        serverCreator.taskService(taskService);

        // 设置默认执行器
        if (serverCreator.getMqttExecutor() == null) {
            serverCreator.mqttExecutor(ThreadUtils.getBizExecutor(ThreadUtils.MAX_POOL_SIZE_FOR_TIO));
        }

        // 构建处理器和服务监听器
        DefaultMqttServerProcessor serverProcessor = new DefaultMqttServerProcessor(serverCreator, taskService, serverCreator.getMqttExecutor());
        TioServerHandler handler = new MqttServerAioHandler(serverCreator, serverProcessor);
        TioServerListener listener = new MqttServerAioListener(serverCreator);

        // 初始化 TioServerConfig
        String serverName = serverCreator.getName();
        if (StrUtil.isBlank(serverName)) {
            throw new IllegalArgumentException("Server name must not be blank.");
        }

        TioServerConfig tioConfig = new TioServerConfig(serverName, handler, listener);
        tioConfig.setUseQueueDecode(serverCreator.isUseQueueDecode());
        tioConfig.setUseQueueSend(serverCreator.isUseQueueSend());

        int readBufferSize = serverCreator.getReadBufferSize();
        if (readBufferSize > 0) {
            tioConfig.setReadBufferSize(readBufferSize);
        }

        tioConfig.statOn = serverCreator.isStatEnable();

        Long heartbeatTimeout = serverCreator.getHeartbeatTimeout();
        if (heartbeatTimeout != null && heartbeatTimeout > 0) {
            tioConfig.setHeartbeatTimeout(heartbeatTimeout);
        }

        tioConfig.setHeartbeatBackoff(serverCreator.getKeepaliveBackoff());
        tioConfig.setHeartbeatMode(HeartbeatMode.LAST_RESP);

        if (serverCreator.getSslConfig() != null) {
            tioConfig.setSslConfig(serverCreator.getSslConfig());
        }

        if (serverCreator.isDebug()) {
            tioConfig.debug = true;
        }

        TioServer tioServer = new TioServer(tioConfig);

        boolean httpEnabled = serverCreator.isHttpEnable();
        boolean wsEnabled = serverCreator.isWebsocketEnable();
        log.info("Mica mqtt http api enable:{} websocket enable:{}", httpEnabled, wsEnabled);

        MqttWebServer webServer = null;
        if (httpEnabled || wsEnabled) {
            webServer = MqttWebServer.config(serverCreator, tioConfig);
        }

        // ⚠️ 注意此处原方法名为 buildMqttServer(...)，疑似递归调用！
        // 应当是一个不同的重载方法，比如 buildMqttServerInstance(...)
        MqttServer mqttServer = buildMqttServerInstance(tioServer, webServer, serverCreator, taskService);

        // 若消息分发器继承自抽象类，则进行额外配置
        if (serverCreator.getMessageDispatcher() instanceof AbstractMqttMessageDispatcher) {
            ((AbstractMqttMessageDispatcher) serverCreator.getMessageDispatcher()).config(mqttServer);
        }

        return mqttServer;
    }


    /**
     * 为MQTT服务器创建器设置默认组件（如果未设置）
     * 当创建器中某些组件未配置时，使用默认实现进行填充，确保服务器能够正常运行
     *
     * @param creator MQTT服务器创建器实例，用于配置和创建MQTT服务器
     */
    // 提取默认组件设置逻辑，增强可读性
    private void setDefaultComponentIfAbsent(MqttServerCreator creator) {
        // 为未配置的组件设置默认实现
        if (creator.getUniqueIdService() == null) {
            creator.uniqueIdService(new DefaultMqttServerUniqueIdServiceImpl());
        }
        if (creator.getMessageDispatcher() == null) {
            creator.messageDispatcher(new DefaultMqttBrokerDispatcher());
        }
        if (creator.getSessionManager() == null) {
            creator.sessionManager(new InMemoryMqttSessionManager());
        }
        if (creator.getMessageStore() == null) {
            creator.messageStore(new InMemoryMqttMessageStore());
        }
        if (creator.getConnectStatusListener() == null) {
            creator.connectStatusListener(new DefaultMqttConnectStatusListener());
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
