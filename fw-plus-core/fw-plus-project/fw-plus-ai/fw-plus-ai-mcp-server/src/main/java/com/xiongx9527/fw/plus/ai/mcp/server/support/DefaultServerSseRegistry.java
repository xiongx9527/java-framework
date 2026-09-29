package com.xiongx9527.fw.plus.ai.mcp.support;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.model.SseSession;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DefaultServerSseRegistry extends ServerSseRegistry {

    /**
     * The MCP server.
     */
    private final McpServer server;

    /**
     * Constructs a new instance of ServerStreamableHttpRegistry.
     *
     * @param server   The MCP server.
     * @param serverId The server ID.
     */
    public DefaultServerSseRegistry(McpServer server, String serverId) {
        super(server, serverId);
        this.server = server;
    }

    /**
     * Sends a message to a specific client.
     *
     * @param uuid The UUID of the client.
     * @param data The data to send.
     */
    @Override
    public void sendMsg(String uuid, String data) {
        super.returnMessage(uuid, data);
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param data The data to broadcast.
     */
    @Override
    public void broadcastMsg(String data) {
        for (Map.Entry<String, SseSession> entry : resolveConnections().entrySet()) {
            try {
                entry.getValue().sendData(data);
            } catch (Exception e) {
                // Handle errors (e.g., remove disconnected clients)
                log.warn("Error sending to client {}: {}", entry.getKey(), e.getMessage());
                removeConnection(entry.getKey(), entry.getValue());
            }
        }
    }


}
