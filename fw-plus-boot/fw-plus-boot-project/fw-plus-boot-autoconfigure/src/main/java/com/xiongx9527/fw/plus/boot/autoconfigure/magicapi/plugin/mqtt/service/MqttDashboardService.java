package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.BaseMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MagicAPIMqttServer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardClientInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardPage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardServerInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardStats;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardSubscriptionRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDataSourceInfo;
import net.dreamlu.iot.mqtt.codec.MqttCodecUtil;
import net.dreamlu.iot.mqtt.codec.MqttVersion;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.MqttServerCreator;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.enums.MessageType;
import net.dreamlu.iot.mqtt.core.server.model.Message;
import net.dreamlu.iot.mqtt.core.server.model.Subscribe;
import net.dreamlu.iot.mqtt.core.util.TopicUtil;
import org.apache.commons.lang3.StringUtils;
import org.tio.core.ChannelContext;
import org.tio.core.Node;
import org.tio.core.Tio;
import org.tio.server.ServerGroupStat;
import org.tio.server.TioServerConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MqttDashboardService {

    public static final String DEFAULT_SERVER_ID = "default";
    private static final String DYNAMIC_SERVER_ID_PREFIX = "datasource:";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final BaseMagicDynamicMqttDataSource dynamicDataSource;
    private final MqttServer defaultServer;

    public MqttDashboardService(BaseMagicDynamicMqttDataSource dynamicDataSource, MqttServer defaultServer) {
        this.dynamicDataSource = dynamicDataSource;
        this.defaultServer = defaultServer;
    }

    public List<MqttDashboardServerInfo> listServers() {
        Map<String, MqttDataSourceInfo> sourceInfo = new LinkedHashMap<String, MqttDataSourceInfo>();
        if (dynamicDataSource != null) {
            for (MqttDataSourceInfo info : dynamicDataSource.datasourceNodes()) {
                if (info != null && StringUtils.isNotBlank(info.getKey())) {
                    sourceInfo.put(info.getKey(), info);
                }
            }
        }

        List<MqttDashboardServerInfo> servers = new ArrayList<MqttDashboardServerInfo>();
        if (isServerRunning(defaultServer)) {
            servers.add(toServerInfo(DEFAULT_SERVER_ID, DEFAULT_SERVER_ID, "默认服务端", defaultServer, true));
        }
        if (dynamicDataSource != null) {
            for (Map.Entry<String, MqttDataSourceInfo> entry : sourceInfo.entrySet()) {
                MagicAPIMqttServer dynamicServer = dynamicDataSource.getServer(entry.getKey());
                if (dynamicServer == null || !isServerRunning(dynamicServer.getMqttServer())) {
                    continue;
                }
                MqttDataSourceInfo info = entry.getValue();
                String name = StringUtils.defaultIfBlank(info.getName(), info.getKey());
                servers.add(toServerInfo(DYNAMIC_SERVER_ID_PREFIX + info.getKey(), info.getKey(), name,
                        dynamicServer.getMqttServer(), false));
            }
        }
        Collections.sort(servers, Comparator.comparing(MqttDashboardServerInfo::isDefaultServer).reversed()
                .thenComparing(MqttDashboardServerInfo::getName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(MqttDashboardServerInfo::getId));
        return servers;
    }

    public MqttDashboardStats stats(String serverId) {
        MqttServer server = requireServer(serverId);
        TioServerConfig config = server.getServerConfig();
        Set<ChannelContext> contexts = Tio.getAll(config);
        ServerGroupStat groupStat = config.getGroupStat() instanceof ServerGroupStat
                ? (ServerGroupStat) config.getGroupStat() : null;

        MqttDashboardStats result = new MqttDashboardStats();
        long receivedPackets = groupStat == null ? 0L : groupStat.getReceivedPackets().sum();
        long receivedBytes = groupStat == null ? 0L : groupStat.getReceivedBytes().sum();
        result.getConnections().put("accepted", groupStat == null ? 0L : groupStat.accepted.sum());
        result.getConnections().put("size", (long) contexts.size());
        result.getConnections().put("closed", groupStat == null ? 0L : groupStat.getClosed().sum());
        result.getMessages().put("handledPackets", groupStat == null ? 0L : groupStat.getHandledPackets().sum());
        result.getMessages().put("handledBytes", groupStat == null ? 0L : groupStat.getHandledBytes().sum());
        result.getMessages().put("receivedPackets", receivedPackets);
        result.getMessages().put("receivedBytes", receivedBytes);
        result.getMessages().put("sendPackets", groupStat == null ? 0L : groupStat.getSentPackets().sum());
        result.getMessages().put("sendBytes", groupStat == null ? 0L : groupStat.getSentBytes().sum());
        result.getMessages().put("bytesPerTcpReceive", receivedPackets == 0 ? 0L : receivedBytes / receivedPackets);
        result.getMessages().put("packetsPerTcpReceive", receivedPackets == 0 ? 0L : 1L);
        result.getNodes().put("clientNodes", (long) contexts.size());
        result.getNodes().put("connections", (long) contexts.size());
        result.getNodes().put("users", contexts.stream().map(ChannelContext::getUserId)
                .filter(StringUtils::isNotBlank).distinct().count());
        result.setSampledAt(System.currentTimeMillis());
        return result;
    }

    public MqttDashboardPage<MqttDashboardClientInfo> clients(String serverId, Integer page, Integer limit) {
        MqttServer server = requireServer(serverId);
        int pageNumber = page == null || page < 1 ? 1 : page;
        int pageSize = limit == null || limit < 1 ? DEFAULT_PAGE_SIZE : Math.min(limit, MAX_PAGE_SIZE);
        List<MqttDashboardClientInfo> clients = Tio.getAll(server.getServerConfig()).stream()
                .filter(context -> StringUtils.isNotBlank(context.getBsId()))
                .sorted(Comparator.comparing(ChannelContext::getBsId))
                .map(context -> toClientInfo(server.getServerCreator(), context))
                .collect(Collectors.toList());
        int from = Math.min((pageNumber - 1) * pageSize, clients.size());
        int to = Math.min(from + pageSize, clients.size());

        MqttDashboardPage<MqttDashboardClientInfo> result = new MqttDashboardPage<MqttDashboardClientInfo>();
        result.setList(new ArrayList<MqttDashboardClientInfo>(clients.subList(from, to)));
        result.setPageNumber(pageNumber);
        result.setPageSize(pageSize);
        result.setTotalRow(clients.size());
        return result;
    }

    public MqttDashboardClientInfo clientInfo(String serverId, String clientId) {
        MqttServer server = requireServer(serverId);
        ChannelContext context = requireClient(server, clientId);
        return toClientInfo(server.getServerCreator(), context);
    }

    public List<Subscribe> subscriptions(String serverId, String clientId) {
        MqttServer server = requireServer(serverId);
        requireClient(server, clientId);
        List<Subscribe> subscriptions = server.getServerCreator().getSessionManager().getSubscriptions(clientId);
        return subscriptions == null ? Collections.<Subscribe>emptyList() : subscriptions;
    }

    public void deleteClient(String serverId, String clientId) {
        MqttServer server = requireServer(serverId);
        requireClient(server, clientId);
        server.close(clientId);
    }

    public void subscribe(String serverId, MqttDashboardSubscriptionRequest request) {
        validateSubscriptionRequest(request, true);
        MqttServer server = requireServer(serverId);
        requireClient(server, request.getClientId());
        dispatchSubscription(server, request, MessageType.SUBSCRIBE);
    }

    public void unsubscribe(String serverId, MqttDashboardSubscriptionRequest request) {
        validateSubscriptionRequest(request, false);
        MqttServer server = requireServer(serverId);
        requireClient(server, request.getClientId());
        dispatchSubscription(server, request, MessageType.UNSUBSCRIBE);
    }

    private MqttDashboardServerInfo toServerInfo(String id, String key, String name, MqttServer server,
                                                 boolean defaultValue) {
        MqttServerCreator creator = server.getServerCreator();
        MqttDashboardServerInfo info = new MqttDashboardServerInfo();
        info.setId(id);
        info.setKey(key);
        info.setName(name);
        info.setIp(StringUtils.defaultString(creator.getIp()));
        info.setPort(creator.getPort());
        info.setWebPort(creator.getWebPort());
        info.setWebsocketEnabled(creator.isWebsocketEnable());
        info.setSslEnabled(creator.getSslConfig() != null);
        info.setRunning(true);
        info.setDefaultServer(defaultValue);
        return info;
    }

    private MqttDashboardClientInfo toClientInfo(MqttServerCreator creator, ChannelContext context) {
        MqttDashboardClientInfo info = new MqttDashboardClientInfo();
        info.setClientId(context.getBsId());
        info.setUsername(context.getUserId());
        info.setConnected(context.isAccepted());
        if (context.heartbeatTimeout == null || creator.getKeepaliveBackoff() <= 0) {
            info.setKeepAlive(60L);
        } else {
            long keepAliveMillis = (long) (context.heartbeatTimeout / (creator.getKeepaliveBackoff() * 2));
            info.setKeepAlive(TimeUnit.MILLISECONDS.toSeconds(keepAliveMillis));
        }
        Node node = context.getClientNode();
        if (node != null) {
            info.setIpAddress(node.getIp());
            info.setPort(node.getPort());
        }
        MqttVersion version = MqttCodecUtil.getMqttVersion(context);
        if (version != null) {
            info.setProtoName(version.protocolName());
            info.setProtoVer(version.protocolLevel());
            info.setProtoFullName(version.fullName());
        }
        info.setSsl(creator.getSslConfig() != null);
        info.setWebSocket(context.containsKey("TIO_W_S_C"));
        info.setCreatedAt(context.stat.timeCreated);
        info.setConnectedAt(context.stat.timeFirstConnected);
        return info;
    }

    private MqttServer requireServer(String serverId) {
        if (StringUtils.isBlank(serverId)) {
            throw new IllegalArgumentException("MQTT 服务端标识不能为空");
        }
        MqttServer server;
        if (DEFAULT_SERVER_ID.equals(serverId)) {
            server = defaultServer;
        } else if (serverId.startsWith(DYNAMIC_SERVER_ID_PREFIX)
                && serverId.length() > DYNAMIC_SERVER_ID_PREFIX.length()) {
            String key = serverId.substring(DYNAMIC_SERVER_ID_PREFIX.length());
            MagicAPIMqttServer dynamicServer = dynamicDataSource == null ? null : dynamicDataSource.getServer(key);
            server = dynamicServer == null ? null : dynamicServer.getMqttServer();
        } else {
            server = null;
        }
        if (!isServerRunning(server)) {
            throw new IllegalArgumentException("MQTT 服务端不存在或未运行: " + serverId);
        }
        return server;
    }

    private boolean isServerRunning(MqttServer server) {
        return server != null
                && server.getTioServer() != null
                && server.getServerConfig() != null
                && !server.getTioServer().isWaitingStop()
                && !server.getServerConfig().isStopped()
                && server.getServerConfig().startTime > 0
                && server.getTioServer().getServerSocketChannel() != null
                && server.getTioServer().getServerSocketChannel().isOpen();
    }

    private ChannelContext requireClient(MqttServer server, String clientId) {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("MQTT 客户端标识不能为空");
        }
        ChannelContext context = server.getChannelContext(clientId);
        if (context == null || context.isClosed()) {
            throw new IllegalArgumentException("MQTT 客户端不存在或已离线: " + clientId);
        }
        return context;
    }

    private void validateSubscriptionRequest(MqttDashboardSubscriptionRequest request, boolean validateQos) {
        if (request == null || StringUtils.isBlank(request.getClientId()) || StringUtils.isBlank(request.getTopic())) {
            throw new IllegalArgumentException("客户端标识和订阅主题不能为空");
        }
        TopicUtil.validateTopicFilter(request.getTopic());
        if (validateQos && (request.getQos() < 0 || request.getQos() > 2)) {
            throw new IllegalArgumentException("MQTT QoS 必须为 0、1 或 2");
        }
    }

    private void dispatchSubscription(MqttServer server, MqttDashboardSubscriptionRequest request, MessageType type) {
        Message message = new Message();
        message.setFromClientId(request.getClientId());
        message.setTopic(request.getTopic());
        message.setQos(request.getQos());
        message.setMessageType(type);
        IMqttMessageDispatcher dispatcher = server.getServerCreator().getMessageDispatcher();
        dispatcher.send(message);
    }
}
