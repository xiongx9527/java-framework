package com.xiongx9527.fw.plus.ai.mcp.spi;

public interface BaseServerRegistry {

    /**
     * Send a message to a specific client on another server.
     *
     * @param serverId The ID of the target server.
     * @param sessionId The session ID of the target client.
     * @param json The JSON data to send.
     */
    void sendOther(String serverId, String sessionId, String json) ;

    /**
     * Broadcast a message to all clients on another server.
     *
     * @param serverId The ID of the target server.
     * @param data The data to broadcast.
     */
    void broadcastOther(String serverId, String data);

}
